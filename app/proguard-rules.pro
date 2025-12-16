# Add project specific ProGuard rules here.
-keep class com.lgh9900.phoneagent.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}