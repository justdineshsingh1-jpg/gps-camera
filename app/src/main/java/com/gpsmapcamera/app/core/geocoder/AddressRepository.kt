package com.gpsmapcamera.app.core.geocoder

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import com.gpsmapcamera.app.core.data.local.AddressDao
import com.gpsmapcamera.app.core.data.local.CachedAddress
import com.gpsmapcamera.app.core.location.OpenLocationCode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class ResolvedAddress(
    val formattedAddress: String,
    val city: String?,
    val state: String?,
    val country: String?,
    val isFromCache: Boolean = false
)

@Singleton
class AddressRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val addressDao: AddressDao
) {
    private val geocoder by lazy { Geocoder(context, Locale.getDefault()) }

    /**
     * Resolves the address in a layered fashion:
     * 1. Online: Native Geocoder with 2-second timeout.
     * 2. Offline: Room database cache if within ~100 meters.
     * 3. Final Fallback: Coordinates + Plus Code.
     */
    suspend fun resolveAddress(
        latitude: Double,
        longitude: Double,
        isOnline: Boolean
    ): ResolvedAddress = withContext(Dispatchers.IO) {
        if (latitude == 0.0 && longitude == 0.0) {
            return@withContext ResolvedAddress("Acquiring GPS...", null, null, null)
        }

        // Layer 1: Online Geocoder (if online)
        if (isOnline && Geocoder.isPresent()) {
            val onlineResult = withTimeoutOrNull(2000L) {
                fetchOnlineAddress(latitude, longitude)
            }
            if (onlineResult != null) {
                // Cache into Room for future offline reuse
                cacheAddress(latitude, longitude, onlineResult)
                return@withContext onlineResult
            }
        }

        // Layer 2: Offline Room Cache within 100 meters
        val cached = findCachedAddressWithin100m(latitude, longitude)
        if (cached != null) {
            return@withContext ResolvedAddress(
                formattedAddress = cached.formattedAddress,
                city = cached.city,
                state = cached.state,
                country = cached.country,
                isFromCache = true
            )
        }

        // Layer 3: Final Fallback (Coordinates + Plus Code)
        val plusCode = OpenLocationCode.encode(latitude, longitude)
        val fallbackAddress = "Plus Code: $plusCode"
        ResolvedAddress(
            formattedAddress = fallbackAddress,
            city = null,
            state = null,
            country = null,
            isFromCache = false
        )
    }

    private suspend fun fetchOnlineAddress(latitude: Double, longitude: Double): ResolvedAddress? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            val addr = addresses.firstOrNull()
                            if (addr != null) {
                                continuation.resume(formatAddress(addr))
                            } else {
                                continuation.resume(null)
                            }
                        }

                        override fun onError(errorMessage: String?) {
                            continuation.resume(null)
                        }
                    })
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                addresses?.firstOrNull()?.let { formatAddress(it) }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun formatAddress(addr: Address): ResolvedAddress {
        val parts = mutableListOf<String>()

        val road = addr.thoroughfare ?: addr.subThoroughfare
        if (!road.isNullOrBlank()) parts.add(road)

        val locality = addr.subLocality ?: addr.featureName
        if (!locality.isNullOrBlank() && locality != road) parts.add(locality)

        val city = addr.locality ?: addr.subAdminArea
        if (!city.isNullOrBlank()) parts.add(city)

        val district = addr.subAdminArea
        if (!district.isNullOrBlank() && district != city) parts.add(district)

        val state = addr.adminArea
        val postal = addr.postalCode
        if (!state.isNullOrBlank()) {
            if (!postal.isNullOrBlank()) parts.add("$state $postal") else parts.add(state)
        }

        val country = addr.countryName
        if (!country.isNullOrBlank()) parts.add(country)

        val formatted = if (parts.isNotEmpty()) {
            parts.joinToString(", ")
        } else {
            addr.getAddressLine(0) ?: "Unknown Location"
        }

        return ResolvedAddress(
            formattedAddress = formatted,
            city = city,
            state = state,
            country = country,
            isFromCache = false
        )
    }

    private suspend fun cacheAddress(lat: Double, lon: Double, resolved: ResolvedAddress) {
        try {
            addressDao.insertAddress(
                CachedAddress(
                    latitude = lat,
                    longitude = lon,
                    formattedAddress = resolved.formattedAddress,
                    city = resolved.city,
                    state = resolved.state,
                    country = resolved.country,
                    timestamp = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun findCachedAddressWithin100m(lat: Double, lon: Double): CachedAddress? {
        val delta = 0.001 // ~111 meters
        val candidates = addressDao.findCandidatesInBoundingBox(
            minLat = lat - delta,
            maxLat = lat + delta,
            minLon = lon - delta,
            maxLon = lon + delta
        )

        val results = FloatArray(1)
        for (candidate in candidates) {
            Location.distanceBetween(lat, lon, candidate.latitude, candidate.longitude, results)
            if (results[0] <= 100.0f) {
                return candidate
            }
        }
        return null
    }
}
