package com.gpsmapcamera.app.di

import android.content.Context
import androidx.room.Room
import com.gpsmapcamera.app.core.data.local.AddressDao
import com.gpsmapcamera.app.core.data.local.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppContext(@ApplicationContext context: Context): Context = context

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "gps_map_camera.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideAddressDao(database: AppDatabase): AddressDao {
        return database.addressDao()
    }
}
