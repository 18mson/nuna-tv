# youtubedl-android & internal dependencies (Apache Commons Compress, Jackson, youtubedl-common)
-keep class com.yausername.** { *; }
-keep class org.apache.commons.** { *; }
-keep class com.fasterxml.jackson.** { *; }
-keepclassmembers class * {
    @com.fasterxml.jackson.annotation.** <fields>;
    @com.fasterxml.jackson.annotation.** <methods>;
}
-dontwarn com.fasterxml.jackson.**
-dontwarn org.apache.commons.**

# WebView JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Media3 ExoPlayer
-keep class androidx.media3.** { *; }
