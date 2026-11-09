# IQ BOT PRO Proguard Rules
-keepattributes *Annotation*
-keepclassmembers class * {
    @org.jetbrains.kotlinx.serialization.Serializable <fields>;
}
-keep class kotlinx.serialization.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
