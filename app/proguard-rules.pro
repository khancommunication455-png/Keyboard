# Add project specific ProGuard rules here.
-keep class com.stylekeyboard.app.data.db.entity.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.JsonClass *;
}
