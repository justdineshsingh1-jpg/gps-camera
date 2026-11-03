# ProGuard / R8 rules for GPS Map Camera
-keep class com.gpsmapcamera.app.** { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
