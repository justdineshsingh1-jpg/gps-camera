package com.gpsmapcamera.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GpsMapCameraApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
    }

    /** App-wide Coil loader that can also decode a frame from videos (thumbnails for GPSVID files). */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .crossfade(true)
            .build()
}
