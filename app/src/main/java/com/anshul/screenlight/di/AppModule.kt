package com.anshul.screenlight.di

import android.content.Context
import android.hardware.SensorManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing application-level dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
        name = "screenlight_preferences"
    )

    /**
     * Provides DataStore<Preferences> singleton for app-wide settings.
     */
    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.dataStore

    /**
     * Provides SensorManager for accessing device sensors.
     */
    @Provides
    @Singleton
    fun provideSensorManager(
        @ApplicationContext context: Context
    ): SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
}
