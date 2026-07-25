# AstraVeil ProGuard rules.
# Minify is disabled in build.gradle.kts for Phase 0, but the file is
# referenced by the release build type — keep the canonical Compose /
# Kotlinx Serialization rules here for when minify is turned on.

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.astraveil.**$$serializer { *; }
-keepclassmembers class com.astraveil.** {
    *** Companion;
}
-keepclasseswithmembers class com.astraveil.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Compose
-dontwarn androidx.compose.**

# AstraVeil core / provider public API
-keep class com.astraveil.core.** { *; }
-keep class com.astraveil.providers.** { *; }
