package com.gpsmapcamera.app.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CachedAddress::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun addressDao(): AddressDao
}
