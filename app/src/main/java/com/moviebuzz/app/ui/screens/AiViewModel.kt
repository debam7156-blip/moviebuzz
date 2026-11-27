package com.moviebuzz.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviebuzz.app.data.db.MovieBuzzDatabase
import com.moviebuzz.app.domain.LibrarySnapshot
import com.moviebuzz.app.domain.MovieForStats
import com.moviebuzz.app.domain.RecommendationEngine
import com.moviebuzz.app.domain.WatchEventForStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AiViewModel(private val db: MovieBuzzDatabase) : ViewModel() {

    private val engine = RecommendationEngine()

    data class SectionUi(val title: String, val recs: List<RecommendationEngine.Recommendation>)
    data class AiUi(
        val sections: List<SectionUi> = emptyList(),
        val conversation: List<String> = emptyList(),
        val needsMoreData: Boolean = false
    )

    private val _ui = MutableStateFlow(AiUi())
    val ui: StateFlow<AiUi> = _ui

    /**
     * Recommendations the user has already acted on in this session (liked or
     * dismissed). Acting on a card removes it from every section so the
     * thumbs-up/down buttons have visible, immediate effect instead of being
     * placeholders that silently recompute the same list.
     */
    private val handled = mutableSetOf<Long>()

    init { refresh() }

    private fun snapshotOf(movies: List<com.moviebuzz.app.data.db.MovieEntity>, events: List<com.moviebuzz.app.data.db.WatchEventEntity>): LibrarySnapshot =
        LibrarySnapshot(
            movies = movies.map {
                MovieForStats(
                    id = it.id, title = it.title, year = it.year,
                    runtimeMinutes = it.runtimeMinutes, director = it.director,
                    genres = it.genres, countries = it.countries,
                    languages = it.languages, cast = it.cast,
                    personalRating = it.personalRating,
                    tmdbVoteAverage = it.tmdbVoteAverage,
                    isFavorite = it.isFavorite, watched = it.watched
                )
            },
            watchEvents = events.map { WatchEventForStats(it.movieId, it.watchedOn, null) }
        )

    fun refresh() {
        viewModelScope.launch {
            val movies = db.movieDao().search("")
            val events = db.watchEventDao().observeAllSnapshot()
            if (movies.count { it.watched } < 5) {
                _ui.value = AiUi(needsMoreData = true)
                return@launch
            }
            val snapshot = snapshotOf(movies, events)
            val watchlistIds = movies.filter { it.watchlist }.map { it.id }.toSet()
            val profile = engine.buildTasteProfile(snapshot, watchlistIds)

            val candidates = movies
                .filter { !it.watched && it.id !in handled }
                .map {
                    RecommendationEngine.Candidate(
                        id = it.id, title = it.title, year = it.year,
                        genres = it.genres, director = it.director, cast = it.cast,
                        countries = it.countries, languages = it.languages,
                        tmdbVoteAverage = it.tmdbVoteAverage,
                        tmdbVoteCount = it.tmdbVoteCount, overview = it.overview
                    )
                }

            _ui.value = AiUi(
                sections = listOf(
                    SectionUi("Your Next Watch", listOfNotNull(engine.nextWatch(profile, candidates))),
                    SectionUi("Made For You", engine.madeForYou(profile, candidates)),
                    SectionUi("Similar to Your Favourites", engine.similarToFavourites(profile, candidates)),
                    SectionUi("Hidden Gems", engine.hiddenGems(profile, candidates)),
                    SectionUi("Directors to Explore", engine.directorsToExplore(profile, candidates)),
                    SectionUi("Underrated Discoveries", engine.underrated(profile, candidates)),
                    SectionUi("Outside Your Comfort Zone", engine.outsideComfortZone(profile, candidates))
                ).filter { it.recs.isNotEmpty() }
            )
        }
    }

    /** 'Helpful' → add the movie to the watchlist and drop it from the sections. */
    fun like(id: Long) {
        if (!handled.add(id)) return
        viewModelScope.launch {
            db.movieDao().setWatchlist(id, true, System.currentTimeMillis())
            refresh()
        }
    }

    /** 'Not for me' → dismiss the movie from this session's sections. */
    fun dislike(id: Long) {
        if (!handled.add(id)) return
        refresh()
    }

    /** Conversational answers grounded in the on-device engines — works offline. */
    fun ask(question: String) {
        viewModelScope.launch {
            val answer = when {
                question.contains("tonight", ignoreCase = true) -> {
                    val next = _ui.value.sections.firstOrNull { it.title == "Your Next Watch" }?.recs?.firstOrNull()
                    next?.let { "Tonight, watch ${it.candidate.title}. Why: ${it.explanation}" }
                        ?: "Add a few more watched films so I can pick well for you."
                }
                question.contains("genre", ignoreCase = true) -> {
                    val movies = db.movieDao().search("")
                    val top = engine.buildTasteProfile(snapshotOf(movies, emptyList()), emptySet()).favouriteGenres.take(3)
                    if (top.isEmpty()) "No ratings yet — once you rate films I can tell your favourite genres."
                    else "Your favourite genres right now: ${top.joinToString(", ") { it.genre }}."
                }
                question.contains("director", ignoreCase = true) -> {
                    _ui.value.sections.firstOrNull { it.title == "Directors to Explore" }
                        ?.let { "Directors worth exploring: " + it.recs.take(5).joinToString(", ") { r -> r.candidate.title } }
                        ?: "Watch a few more films and I will map your director preferences."
                }
                question.contains("different", ignoreCase = true) || question.contains("comfort", ignoreCase = true) -> {
                    _ui.value.sections.firstOrNull { it.title == "Outside Your Comfort Zone" }
                        ?.let { "Try: " + it.recs.take(3).joinToString(", ") { r -> r.candidate.title } }
                        ?: "Once your taste profile is built, I will suggest films outside your usual genres."
                }
                question.contains("taste", ignoreCase = true) || question.contains("analys", ignoreCase = true) -> {
                    val movies = db.movieDao().search("")
                    val profile = engine.buildTasteProfile(snapshotOf(movies, emptyList()), emptySet())
                    "You have watched ${movies.count { it.watched }} films. " +
                        "Average rating ${profile.averageRating?.let { "%.1f".format(it) } ?: "—"}. " +
                        "Favourite genres: ${profile.favouriteGenres.take(3).joinToString(", ") { it.genre }}."
                }
                else -> "Ask me things like: What should I watch tonight? Which genres do I enjoy most? Analyse my movie taste."
            }
            _ui.value = _ui.value.copy(conversation = _ui.value.conversation + listOf("You: $question", "MOVIEBUZZ: $answer"))
        }
    }
}
