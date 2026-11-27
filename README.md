# 🎬 MOVIEBUZZ

### Your personal movie universe.

MOVIEBUZZ is a premium, offline-first **movie discovery and tracking** app for Android.
Browse what's trending, track everything you watch, build collections, rate films, and get
personalised recommendations — with a dark cinematic Material 3 UI.

---

## ✨ What's inside

| Area | What you get |
|---|---|
| **Home** | Trending, popular, recently released, upcoming and personalised recommendation feeds |
| **Discover** | Tabbed feeds: Trending, Popular, Upcoming, Top rated, In theaters |
| **Search** | Full TMDB title search with debounce, skeletons and friendly errors; falls back to your local library when offline |
| **Movie Details** | Backdrop hero, poster, tagline, genres, runtime, director, TMDB rating, cast with characters & headshots, YouTube trailer, similar + recommended movies |
| **Watchlist** | One-tap add/remove, persisted locally in Room |
| **Watched** | Mark watched with automatic viewing history, personal 1–10 star ratings, written reviews |
| **Collections** | Create, rename, edit and delete custom collections; add/remove movies from any movie's detail page |
| **Library** | All personal movies with search, Watched / Watchlist / Favourites filters and sorting |
| **Statistics** | Films watched, sessions, watch hours, average rating, streaks, genre distribution, rating histogram, most rewatched, top directors/actors — all computed from your real data, never fabricated |
| **MOVIEBUZZ AI** | 7 personalised on-device recommendation sections (*Made For You, Hidden Gems, Similar to Your Favourites, Directors to Explore, Underrated Discoveries, Outside Your Comfort Zone, Your Next Watch*) with human-readable explanations and a conversational assistant |
| **Import Engine** | Letterboxd ZIP/CSV import, duplicate-safe and rewatch-aware, with a manual resolution queue for uncertain matches and a detailed import report |
| **Backup** | Full JSON export/restore of your library, ratings, history, collections and tags |
| **Offline-first** | Room local database; watchlists, ratings, history, collections, stats and AI all work with no internet; TMDB metadata is cached locally |

## 🎨 Brand

- Minimalist **dark-first** design: deep black & charcoal surfaces, elegant typography, **gold** accent.
- Skeleton loaders, purposeful empty states, error states with retry, bottom navigation — no clutter, no placeholder buttons.

## 🛠 Tech stack

- **Kotlin** + **Jetpack Compose** (Material 3), single-Activity architecture
- **Room** database (v2, with migration) with `Flow`-based reactive queries
- **Retrofit + OkHttp + kotlinx-serialization** for TMDB, **Coil** for lazy image loading & caching
- **MVVM**: every screen's ViewModel is built by `MovieBuzzViewModelFactory` with real dependencies
- Pure-Kotlin, fully unit-testable domain layer (`StatsEngine`, `RecommendationEngine`, `ImportEngine`)
- TMDB feed cache with TTL and stale-cache fallback when offline

## 📁 Project structure

```
app/src/main/java/com/moviebuzz/app/
├── data/           Room DB, TMDB repo, import engine, backup
├── domain/         StatsEngine, RecommendationEngine (pure Kotlin, unit-tested)
├── ui/             theme, components, nav, screens (MVVM)
app/src/test/       Unit tests: CSV parsing, adapters, stats, recommendations
docs/               API key guide
.github/workflows/  CI (APK build)
```

## 🚀 Quick start

1. **Prerequisites**: Android Studio (Koala or newer), JDK 17, device/emulator API 26+.
2. **TMDB API key** (free, needed only for online discovery): create an account at https://www.themoviedb.org/ → Settings → API → request a v3 developer key.
3. **Configure the key — two options**:
   - **At runtime (recommended, no rebuild)**: install the app, open **Settings**, paste your key, tap **Save key**. Stored only on your device via DataStore.
   - **At build time**: create `local.properties` in the repo root with `MOVIEBUZZ_TMDB_API_KEY=your_key` (Git-ignored), or set the `MOVIEBUZZ_TMDB_API_KEY` secret in GitHub Actions. See `.env.example` and `docs/API_KEYS.md`.
4. **Build & run**: open in Android Studio and press Run, or from CLI with Gradle 8.9+:

```bash
gradle wrapper --gradle-version 8.9   # one-time, generates ./gradlew
./gradlew assembleDebug               # debug APK at app/build/outputs/apk/debug/
./gradlew test                        # unit tests
```

**GitHub Actions** builds the APK on every push — download it from the run's *Artifacts* (`.github/workflows/apk.yml`).

Without a TMDB key the app still launches and every personal feature (watchlist, watched, ratings, collections, stats, AI, import, backup) works — you just won't see online discovery feeds until you add a key.

## 📥 Import formats — honest status

| Source | Status |
|---|---|
| **Letterboxd** (official ZIP: watched/ratings/diary/watchlist/reviews/lists CSVs) | ✅ Implemented and unit-tested against fixtures mirroring the real published export columns |
| **Moviebase** | ⚠️ Implemented via flexible CSV adapter — **pending validation against a real export file** |
| **I Like Movies** | ⚠️ Implemented via flexible CSV adapter — **pending validation against a real export file** |
| **Generic CSV** | ✅ Flexible header mapping, auto delimiter & rating-scale detection |

The import engine **never silently discards records**: unmatched rows are saved to the `import_unresolved` table for review.

## 🔐 Security

- API keys live in `local.properties` / CI secrets / on-device DataStore only — never committed.
- ZIP extraction validates entry paths (no path traversal) and caps uncompressed size.
- Backup restore is merge-aware and never overwrites your data with blanks.

## 🧪 Testing

`./gradlew test` covers: CSV parsing (quotes, CRLF, BOM, delimiters), the flexible CSV adapter (header mapping, rating-scale normalisation), statistics math (nulls, rewatches, streaks, genre averages) and recommendation ranking (watched exclusion, genre affinity, sparse-history and popular-but-unrelated guards).

## ⚠️ Known limitations (honest list)

- Moviebase & I Like Movies adapters await validation on real export files (upload one to finalize).
- The Gradle wrapper JAR is not committed (binaries excluded); run `gradle wrapper` once or let Android Studio generate it.
- CI builds the APK and uploads it as an artifact; wiring the new `test` step into `.github/workflows/apk.yml` requires the repo owner's GitHub token to have the `workflow` scope (or paste the updated workflow file manually — see the delivery report).

---

Built from scratch as **MOVIEBUZZ** — an independent product. 🍿
