# AstraVeil ProGuard Rules

# Keep all Kotlin serialization classes
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep AstraVeil serializable models
-keep,includedescriptorclasses class com.astraveil.**$$serializer { *; }
-keepclassmembers class com.astraveil.** {
    *** Companion;
}
-keepclasseswithmembers class com.astraveil.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep JNI native methods
-keepclasseswithmembernames class com.astraveil.nativelib.** {
    native <methods>;
}

# Keep Compose
-dontwarn androidx.compose.**

# Keep protobuf
-keep class com.astraveil.ipc.protobuf.** { *; }
