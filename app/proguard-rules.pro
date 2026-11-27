# Cinedor R8 rules.
# Goal: make the APK small by removing unused LIBRARY code (icon packs, unused parts of
# Firebase, Media3, OkHttp...). The app's own code is kept whole, so nothing that the app looks
# up by name (JSON fields, Room entities, WebView bridges, notification layouts) can break.

-keep class com.example.** { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Readable crash reports (the Crash report in the admin Feedback list).
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod

# Libraries that reference optional classes which are not in the app.
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn javax.annotation.**
-dontwarn com.google.errorprone.annotations.**
