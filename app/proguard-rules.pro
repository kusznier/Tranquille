# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keepattributes *Annotation*
-keepclassmembers class * {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

# And if you use AsyncExecutor:
-keepclassmembers class * extends org.greenrobot.eventbus.util.ThrowableFailureEvent {
    <init>(java.lang.Throwable);
}

-keep class com.android.internal.telephony.ITelephony { *; }

-dontobfuscate

# --- Kept for R8 (release minification) -------------------------------------
# Logging: logback-android builds its appenders, encoders and pattern converters
# from assets/logback.xml by class name, and slf4j finds logback by ServiceLoader.
-keep class ch.qos.logback.** { *; }
-keep class org.slf4j.** { *; }
-dontwarn ch.qos.logback.**
-dontwarn org.slf4j.**

# Conscrypt: native code calls back into these classes by name.
-keep class org.conscrypt.** { *; }
-dontwarn org.conscrypt.**

# Preference classes are inflated from res/xml by name (Settings.init defaults).
-keep public class * extends androidx.preference.Preference {
    public <init>(android.content.Context, android.util.AttributeSet);
}

# WorkManager (database updates) creates its Room database, its own workers and
# input mergers by class name. Without this R8 removes WorkDatabase_Impl's
# constructor and the app crashes on start in androidx.startup.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.InputMerger { <init>(); }

# LibPhoneNumberInfo (the :sia module) is ours and small; keep it as is.
-keep class dummydomain.yetanothercallblocker.sia.** { *; }

# Optional platform classes referenced by OkHttp and commons-csv.
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn javax.annotation.**

