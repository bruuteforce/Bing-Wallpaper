# Add project specific R8 rules here.
# AGP will combine all keep rule files in src/main/keepRules to pass to R8
#
# For more details, see
#   https://d.android.com/r/tools/r8/keep-rules

# Moshi
-keep @com.squareup.moshi.JsonClass public class *
-keepclassmembers public class * {
    @com.squareup.moshi.Json <fields>;
}

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class * implements kotlinx.serialization.KSerializer {
    *;
}
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# OkHttp & Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# Coil
-dontwarn coil.**
-keep class coil.** { *; }
