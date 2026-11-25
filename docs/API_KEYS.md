# API Keys and Security

MOVIEBUZZ needs exactly one key for online features (all personal features work without it).

## 1. TMDB key (required for Discover / metadata)

1. Create a free account at https://www.themoviedb.org/
2. Settings → API → Request an API Key (Developer) → accept the terms.
3. Copy the v3 API key (32 hex characters).

## Where to put it

GitHub Actions: Settings → Secrets and variables → Actions → new secret named
`MOVIEBUZZ_TMDB_API_KEY` with your key as the value. The CI build injects it.

Local builds: create `local.properties` in the repo root (Git-ignored):

    MOVIEBUZZ_TMDB_API_KEY=your_32_char_key

## Rules

- Never commit keystores, tokens, or key-bearing files.
- `local.properties` and `.env` files are Git-ignored (except `.env.example`).
