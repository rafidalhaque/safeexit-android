# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep DeviceAdminReceiver subclass members
-keepclassmembers class * extends android.app.admin.DeviceAdminReceiver {
    public *;
}

# Keep the SafeExit DeviceAdminReceiver
-keep class com.teamsabily.safeexit.receiver.SafeExitDeviceAdminReceiver { *; }
