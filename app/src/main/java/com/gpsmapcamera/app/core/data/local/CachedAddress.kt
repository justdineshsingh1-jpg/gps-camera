package com.gpsmapcamera.app.core.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cached_addresses",
    indices = [
        Index(value = ["latitude", "longitude"]),
        Index(value = ["timestamp"])
    ]
)
data class CachedAddress(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val formattedAddress: String,
    val road: String? = null,
    val locality: String? = null,
    val city: String? = null,
    val district: String? = null,
    val state: String? = null,
    val postcode: String? = null,
    val country: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
