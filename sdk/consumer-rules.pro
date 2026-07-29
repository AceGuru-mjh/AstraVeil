# Consumer ProGuard rules for the :sdk module.
#
# These rules are applied to any app that consumes :sdk as a dependency.
# Keep rules that must survive minification in the consuming app go here.

# Keep the public SDK surface — third-party .avm modules link against it.
-keep public class com.astraveil.sdk.** { public *; }
-keepclassmembers public class com.astraveil.sdk.AstraSdkConstants {
    public static *;
}
