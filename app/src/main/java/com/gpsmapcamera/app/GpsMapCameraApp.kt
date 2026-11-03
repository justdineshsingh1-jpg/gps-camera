package com.gpsmapcamera.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GpsMapCameraApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
