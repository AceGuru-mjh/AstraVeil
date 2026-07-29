# Consumer ProGuard rules for the :modules module.
#
# These rules are applied to any app that consumes :modules as a dependency.

# Keep the module manifest serializer (kotlinx-serialization reflection).
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.astraveil.modules.**$$serializer { *; }
-keepclassmembers class com.astraveil.modules.** {
    *** Companion;
}
-keepclasseswithmembers class com.astraveil.modules.** {
    kotlinx.serialization.KSerializer serializer(...);
}
