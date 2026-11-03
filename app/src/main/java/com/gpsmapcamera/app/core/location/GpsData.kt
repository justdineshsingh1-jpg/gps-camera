package com.gpsmapcamera.app.core.location

data class GpsData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitudeMeters: Double = 0.0,
    val accuracyMeters: Float = 0.0f,
    val speedMps: Float = 0.0f,
    val bearingDegrees: Float = 0.0f,
    val compassDegrees: Float = 0.0f,
    val compassDirection: String = "N",
    val plusCode: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isLocked: Boolean = false,
    val satellitesCount: Int = 0,
    val addressLine: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null
)
