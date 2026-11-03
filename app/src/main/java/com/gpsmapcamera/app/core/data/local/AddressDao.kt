package com.gpsmapcamera.app.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AddressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddress(address: CachedAddress): Long

    /**
     * Bounding box search to quickly filter candidate addresses near the target coordinates.
     * ±0.001 degrees is ~111 meters.
     */
    @Query("""
        SELECT * FROM cached_addresses 
        WHERE latitude BETWEEN :minLat AND :maxLat 
          AND longitude BETWEEN :minLon AND :maxLon
        ORDER BY timestamp DESC
        LIMIT 10
    """)
    suspend fun findCandidatesInBoundingBox(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double
    ): List<CachedAddress>

    @Query("SELECT * FROM cached_addresses ORDER BY timestamp DESC LIMIT 50")
    suspend fun getAllRecentAddresses(): List<CachedAddress>

    @Query("DELETE FROM cached_addresses WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long)
}
