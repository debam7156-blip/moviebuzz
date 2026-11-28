# ---- MOVIEBUZZ release build rules ----
# R8/ProGuard configuration to prevent runtime crashes in minified builds.

# Kotlinx serialization: keep generated serializers and serializable classes.
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.moviebuzz.app.**$$serializer { *; }
-keepclassmembers class com.moviebuzz.app.** { *** Companion; }
-keepclasseswithmembers class com.moviebuzz.app.** { kotlinx.serialization.KSerializer serializer(...); }

# Retrofit suspends functions with kotlinx-serialization converter.
-keepattributes AnnotationDefault
-keepclassmembers,allowshrinking,allowobfuscation interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keepclassmembers,allowshrinking,allowobfuscation class kotlinx.serialization.json.Json { kotlinx.serialization.json.Json$Key get$...(); }

# Retrofit / OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn javax.annotation.**
-keep,allowobfuscation,allowshrinking class retrofit2.KotlinExtensions
-keep,allowobfuscation,allowshrinking class retrofit2.KotlinExtensions$*
-keepclassmembers,allowshrinking,allowobfuscation class kotlin.coroutines.SafeContinuation { volatile kotlin.Any result; }

# Room: keep entities and type converters (accessed via reflection by generated code).
-keep class com.moviebuzz.app.data.db.** { *; }

# TMDB models deserialized by kotlinx-serialization.
-keep class com.moviebuzz.app.data.tmdb.** { *; }
-keep class com.moviebuzz.app.data.sync.** { *; }
-keep class com.moviebuzz.app.data.import.** { *; }
