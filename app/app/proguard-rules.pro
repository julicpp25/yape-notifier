# Reglas ProGuard / R8 para NotiCobros (Release Google Play)

# OkHttp3
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-keepclassmembers class * extends okhttp3.ResponseBody { *; }

# Google Firebase Cloud Messaging
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keepattributes *Annotation*,InnerClasses,EnclosingMethod

# Google ZXing (Generador de QR)
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# Kotlin Coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Modelos JSON y SharedPreferences
-keepclassmembers class * {
    @org.json.** <fields>;
}
