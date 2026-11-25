# Build the MOVIEBUZZ debug APK with GitHub Actions

Add this file manually (the automated push of workflow files is restricted):

Path: .github/workflows/apk.yml

```yaml
name: MovieBuzz APK

on:
  push:
    branches: [main, feature/phase1-architecture]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17
      - uses: gradle/actions/setup-gradle@v3
      - name: Build debug APK
        env:
          MOVIEBUZZ_TMDB_API_KEY: ${{ secrets.MOVIEBUZZ_TMDB_API_KEY }}
        run: |
          curl -sL https://services.gradle.org/distributions/gradle-8.9-bin.zip -o g.zip
          unzip -q g.zip
          gradle-8.9/bin/gradle wrapper --gradle-version 8.9
          ./gradlew assembleDebug
      - uses: actions/upload-artifact@v4
        with:
          name: moviebuzz-debug-apk
          path: app/build/outputs/apk/debug/app-debug.apk
```

Steps:
1. In your moviebuzz repo, click "Add file" then "Create new file".
2. Name it exactly: .github/workflows/apk.yml
3. Copy everything between the yaml fences above (not the fences themselves) into the file.
4. Commit.
5. Optional but recommended: add a repository secret named MOVIEBUZZ_TMDB_API_KEY
   (Settings → Secrets and variables → Actions → New repository secret) with your free TMDB key
   from https://www.themoviedb.org/settings/api so Discover works in the app.
6. Go to the Actions tab — a build will start automatically. When the green check appears,
   open that run and download "moviebuzz-debug-apk".
