package com.gpsmapcamera.app.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationTracker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val compassSensor: CompassSensor
) {
    private val fusedClient by lazy { LocationServices.getFusedLocationProviderClient(context) }
    private val locationManager by lazy { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }

    @SuppressLint("MissingPermission")
    fun getLocationUpdates(intervalMs: Long = 1000L): Flow<GpsData> = callbackFlow {
        var latestCompass = 0.0f
        var latestGpsData = GpsData()

        fun updateAndEmit(loc: Location) {
            val plusCode = OpenLocationCode.encode(loc.latitude, loc.longitude)
            val compassDir = LocationFormattingUtils.getCompassDirection(latestCompass)
            val isMock = MockLocationDetector.isLocationMock(loc) || MockLocationDetector.isMockLocationActive(context, loc)

            latestGpsData = latestGpsData.copy(
                latitude = loc.latitude,
                longitude = loc.longitude,
                altitudeMeters = if (loc.hasAltitude()) loc.altitude else 0.0,
                accuracyMeters = if (loc.hasAccuracy()) loc.accuracy else 0.0f,
                speedMps = if (loc.hasSpeed()) loc.speed else 0.0f,
                bearingDegrees = if (loc.hasBearing()) loc.bearing else latestCompass,
                compassDegrees = latestCompass,
                compassDirection = compassDir,
                plusCode = plusCode,
                timestamp = loc.time.takeIf { it > 0 } ?: System.currentTimeMillis(),
                isLocked = true,
                isMockLocation = isMock
            )
            trySend(latestGpsData)
        }

        // Immediately emit cached/last known location to prevent Lat 0.000000 on startup
        try {
            fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
                if (lastLoc != null) updateAndEmit(lastLoc)
            }
            val lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (lastKnown != null) {
                updateAndEmit(lastKnown)
            }
        } catch (ignored: Exception) {}

        // Try FusedLocationProviderClient first
        var usedFused = false
        val fusedCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { updateAndEmit(it) }
            }
        }

        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                updateAndEmit(location)
            }
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
                .setMinUpdateIntervalMillis(500L)
                .build()

            fusedClient.requestLocationUpdates(locationRequest, fusedCallback, Looper.getMainLooper())
                .addOnSuccessListener {
                    usedFused = true
                }
                .addOnFailureListener {
                    // Fallback directly to LocationManager
                    startLocationManagerFallback(locationListener, intervalMs)
                }
        } catch (e: Exception) {
            startLocationManagerFallback(locationListener, intervalMs)
        }

        awaitClose {
            if (usedFused) {
                fusedClient.removeLocationUpdates(fusedCallback)
            }
            try {
                locationManager.removeUpdates(locationListener)
            } catch (ignored: Exception) {}
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationManagerFallback(listener: LocationListener, intervalMs: Long) {
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    intervalMs,
                    0f,
                    listener,
                    Looper.getMainLooper()
                )
            } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    intervalMs,
                    0f,
                    listener,
                    Looper.getMainLooper()
                )
            }
        } catch (e: Exception) {
            // Log or fallback
        }
    }
}
