# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# TWA 관련 클래스 유지
-keep class com.google.androidbrowserhelper.** { *; }
-keep class androidx.browser.** { *; }

# AndroidX 관련
-keep class androidx.core.** { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep custom view constructors
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

# Keep R class
-keepclassmembers class **.R$* {
    public static <fields>;
}
