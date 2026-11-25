# 🎬 MOVIEBUZZ

### Your personal movie universe.

MOVIEBUZZ is a premium, AI-powered **movie tracking and personal movie intelligence** app for Android.
Track everything you watch, import your existing history from other apps, explore rich movie metadata,
dive into deep personal statistics, and get recommendations that genuinely understand your taste.

---

## ✨ What's inside

| Area | What you get |
|---|---|
| **Personal Library** | Watched status, personal ratings, watch dates, full rewatch history, reviews, notes, favourites, watchlist, custom tags, custom lists |
| **Import Engine** | Letterboxd ZIP/CSV import with tested adapters, plus a flexible CSV adapter for Moviebase, I Like Movies and generic exports. Duplicate-safe, rewatch-aware, with a manual resolution flow for uncertain matches and a detailed import report |
| **Discovery** | TMDB-powered trending, popular, upcoming and top-rated browsing with pagination and offline metadata caching |
| **Movie Details** | Posters, cinematic backdrops, overview, cast, genres, runtime, release date, production info, external ratings, related movies |
| **Statistics** | Total movies, viewing sessions, watch hours, average rating, most rewatched films, genre distribution & preferences over time, monthly trends, activity calendar, viewing streaks, ranking stats — all computed from your real data, never fabricated |
| **MOVIEBUZZ AI** | 7 personalized recommendation sections (*Made For You, Hidden Gems, Similar to Your Favourites, Directors to Explore, Underrated Discoveries, Outside Your Comfort Zone, Your Next Watch*) with human-readable explanations, feedback controls, and a conversational assistant. Fully works on-device with an optional LLM proxy |
| **Offline-first** | Room local database; the app is fully usable with no internet. Cached posters, local stats, local tracking |
| **Backup & Sync** | JSON backup export/restore, and an optional cloud-sync worker with last-write-wins + field-level merge conflict resolution |

## 🎨 Brand

- Minimalist **dark-first** design: deep black & charcoal surfaces, elegant typography, **gold** accent.
- Smooth animations, skeleton loaders, empty states, consistent icons — no clutter, no placeholder buttons.

## 🛠 Tech stack

- **Kotlin** + **Jetpack Compose** (Material 3), single-Activity architecture
- **Room** database with `Flow`-based reactive queries
- **Retrofit + OkHttp + kotlinx-serialization** for TMDB, **Coil** for lazy image loading & caching
- **WorkManager** for background sync/backup
- Pure-Kotlin, fully unit-testable domain layer (`StatsEngine`, `RecommendationEngine`, `ImportEngine`) behind interfaces

## 📁 Project structure

```
app/src/main/java/com/moviebuzz/app/
├── data/           Room DB, TMDB repo, import engine, sync
├── domain/         StatsEngine, RecommendationEngine, AiAssistant
├── ui/             theme, components, nav, screens
app/src/test/       Unit tests: CSV parsing, adapters, dedup, matching, stats, recommendations
docs/               Architecture, database, import formats, API key guide
.github/workflows/  CI (tests) + Android APK build
```

## 🚀 Quick start

1. **Prerequisites**: Android Studio (Koala or newer), JDK 17, device/emulator API 26+.
2. **TMDB API key** (free): create an account at https://www.themoviedb.org/ → Settings → API → request a v3 developer key.
3. **Configure keys** (never committed): create `local.properties` in the repo root with `MOVIEBUZZ_TMDB_API_KEY=your_key` (see `.env.example` and `docs/API_KEYS.md`).
4. **Build & run**: open in Android Studio and press Run, or from CLI with Gradle 8.9+:

```bash
gradle assembleDebug   # debug APK at app/build/outputs/apk/debug/
gradle test            # unit tests
```

**GitHub Actions** builds the APK on every push — download it from the run's *Artifacts* (`.github/workflows/android-build.yml`). Release-signing template included.

### Release APK
In Android Studio: Build → Generate Signed Bundle/APK → create a keystore, or use the commented release job in `android-build.yml` with repo secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`), or `gradle assembleRelease`.

## 📥 Import formats — honest status

| Source | Status |
|---|---|
| **Letterboxd** (official ZIP: watched/ratings/diary/watchlist/reviews/lists CSVs) | ✅ Implemented and unit-tested against fixtures mirroring the real published export columns |
| **Moviebase** | ⚠️ Implemented via flexible CSV adapter — **pending validation against a real export file** |
| **I Like Movies** | ⚠️ Implemented via flexible CSV adapter — **pending validation against a real export file** |
| **Generic CSV** | ✅ Flexible header mapping (title/year/rating/date/review/favourite/watchlist/TMDB-ID/IMDb-ID/list/rewatch), auto delimiter & rating-scale detection |

The import engine **never silently discards records**: unmatched or failed rows are surfaced in a manual resolution screen for you to fix. Real sample exports for Moviebase and I Like Movies will finalize their dedicated adapters.

## 🔐 Security

- API keys live in `local.properties` / environment variables only — injected at build time, never committed.
- AI and cloud-sync calls go through **your own backend endpoint**; no AI key ships inside the APK.
- ZIP extraction validates entry paths (no path traversal). CSV size/structure validated before import.
- Full data export and complete data deletion are built into Settings.

## 🧪 Testing

`gradle test` covers: CSV parsing (quotes, CRLF, BOM, delimiters), Letterboxd adapter, duplicate detection, rewatch history, custom lists, match scoring, import report correctness, statistics math (nulls, rewatches), recommendation ranking & watched-exclusion.

## ⚠️ Known limitations (honest list)

- Moviebase & I Like Movies adapters await validation on real export files (upload one to finalize).
- Cloud sync targets a simple self-hosted JSON endpoint (contract documented) rather than a proprietary cloud.
- Gradle wrapper JAR not committed (binaries excluded); Android Studio or `gradle wrapper` generates it.

---

Built from scratch as **MOVIEBUZZ** — an independent product. 🍿
