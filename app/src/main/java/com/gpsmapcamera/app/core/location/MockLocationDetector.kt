package com.gpsmapcamera.app.core.location

import android.app.AppOpsManager
import android.content.Context
import android.location.Location
import android.os.Build
import android.os.Process
import android.provider.Settings

/**
 * Detects whether GPS coordinates are being spoofed by mock location apps
 * (e.g. Fake GPS, GPS JoyStick, Developer Options Mock Providers).
 */
object MockLocationDetector {

    /**
     * Inspects a [Location] object directly to determine if it originated from a mock provider.
     */
    fun isLocationMock(location: Location?): Boolean {
        if (location == null) return false

        // Android 12+ (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (location.isMock) return true
        } else {
            // Android 4.3 - 11 (API 18 - 30)
            @Suppress("DEPRECATION")
            if (location.isFromMockProvider) return true
        }

        // Check location extras for mockLocation flag
        try {
            val extras = location.extras
            if (extras != null && extras.getBoolean("mockLocation", false)) {
                return true
            }
        } catch (_: Exception) {}

        return false
    }

    /**
     * Comprehensive check combining location flags, AppOps permission, and system settings.
     */
    fun isMockLocationActive(context: Context, location: Location? = null): Boolean {
        if (isLocationMock(location)) return true

        // AppOps check: checks if mock location permission has been allowed
        try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            if (appOps != null) {
                val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    appOps.unsafeCheckOpNoThrow(
                        AppOpsManager.OPSTR_MOCK_LOCATION,
                        Process.myUid(),
                        context.packageName
                    )
                } else {
                    @Suppress("DEPRECATION")
                    appOps.checkOpNoThrow(
                        AppOpsManager.OPSTR_MOCK_LOCATION,
                        Process.myUid(),
                        context.packageName
                    )
                }
                if (mode == AppOpsManager.MODE_ALLOWED) {
                    return true
                }
            }
        } catch (_: Exception) {}

        // Settings.Secure check for legacy devices
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            try {
                @Suppress("DEPRECATION")
                val isMock = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ALLOW_MOCK_LOCATION
                )
                if (isMock == "1") return true
            } catch (_: Exception) {}
        }

        return false
    }
}
