package com.moviebuzz

import com.moviebuzz.app.domain.LibrarySnapshot
import com.moviebuzz.app.domain.MovieForStats
import com.moviebuzz.app.domain.RecommendationEngine
import com.moviebuzz.app.domain.StatsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationEngineTest {

    private val engine = RecommendationEngine(StatsEngine())

    private fun watchedMovie(id: Long, title: String, genres: List<String>, rating: Double, director: String? = "D", cast: List<String> = listOf("A")) =
        MovieForStats(id, title, 2020, 100, director, genres, listOf("USA"), listOf("English"), cast, rating, 7.0, true, true)

    private fun candidate(id: Long, title: String, genres: List<String>, votes: Int = 500, avg: Double = 7.0, director: String? = "New Director") =
        RecommendationEngine.Candidate(id, title, 2021, genres, director, listOf("A"), listOf("USA"), listOf("English"), avg, votes, "overview")

    private fun profile(vararg watched: MovieForStats) =
        engine.buildTasteProfile(LibrarySnapshot(watched.toList(), emptyList()), watchlistIds = emptySet())

    @Test
    fun `watched movies are never recommended`() {
        val profile = profile(watchedMovie(1, "Loved Drama", listOf("Drama"), 9.0))
        val candidates = listOf(candidate(1, "Loved Drama", listOf("Drama")))
        assertTrue(engine.madeForYou(profile, candidates).isEmpty())
    }

    @Test
    fun `genre affinity drives ranking`() {
        val profile = profile(
            watchedMovie(1, "Drama One", listOf("Drama"), 9.0),
            watchedMovie(2, "Drama Two", listOf("Drama"), 8.5)
        )
        val candidates = listOf(
            candidate(10, "Sci-Fi Thing", listOf("Sci-Fi")),
            candidate(11, "More Drama", listOf("Drama"))
        )
        val recs = engine.madeForYou(profile, candidates)
        assertEquals("More Drama", recs.first().candidate.title)
    }

    @Test
    fun `explanations state the actual reason`() {
        val profile = profile(watchedMovie(1, "Drama One", listOf("Drama"), 9.0))
        val recs = engine.madeForYou(profile, listOf(candidate(11, "More Drama", listOf("Drama"))))
        assertTrue(recs.first().explanation.contains("Drama"))
    }

    @Test
    fun `hidden gems require low votes and good rating`() {
        val profile = profile(watchedMovie(1, "Drama One", listOf("Drama"), 9.0))
        val candidates = listOf(
            candidate(20, "Blockbuster", listOf("Drama"), votes = 50_000, avg = 8.0),
            candidate(21, "Indie Gem", listOf("Drama"), votes = 300, avg = 7.2)
        )
        val gems = engine.hiddenGems(profile, candidates)
        assertEquals(listOf("Indie Gem"), gems.map { it.candidate.title })
    }

    @Test
    fun `sparse history yields no recommendations instead of guesses`() {
        val profile = profile(watchedMovie(1, "Only Film", listOf("Drama"), 7.0))
        val candidates = listOf(candidate(30, "Unrelated", listOf("War")))
        assertTrue(engine.madeForYou(profile, candidates).isEmpty())
    }
}
