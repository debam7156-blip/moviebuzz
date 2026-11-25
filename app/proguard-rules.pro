# Keep kotlinx.serialization, Retrofit models
-keepclassmembers class com.moviebuzz.app.data.tmdb.** { *; }
-keepclassmembers class com.moviebuzz.app.data.sync.** { *; }

# Retrofit / OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
