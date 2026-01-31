package com.anshul.screenlight.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.anshul.screenlight.data.model.ScreenSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsRepositoryTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: SettingsRepository

    @Before
    fun setup() {
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tempFolder.newFile("test_settings.preferences_pb") }
        )
        repository = SettingsRepository(dataStore)
    }

    @Test
    fun `initial settings are default values`() = testScope.runTest {
        val settings = repository.settings.first()
        assertEquals(ScreenSettings.DEFAULT_BRIGHTNESS, settings.brightness, 0.001f)
        assertEquals(ScreenSettings.DEFAULT_COLOR_TEMPERATURE, settings.colorTemperature, 0.001f)
    }

    @Test
    fun `updateBrightness persists value`() = testScope.runTest {
        repository.updateBrightness(0.75f)
        val settings = repository.settings.first()
        assertEquals(0.75f, settings.brightness, 0.001f)
    }

    @Test
    fun `updateBrightness coerces value to 0-1 range`() = testScope.runTest {
        repository.updateBrightness(1.5f)
        val settings = repository.settings.first()
        assertEquals(1.0f, settings.brightness, 0.001f)

        repository.updateBrightness(-0.5f)
        val settingsAfter = repository.settings.first()
        assertEquals(0.0f, settingsAfter.brightness, 0.001f)
    }

    @Test
    fun `updateColorTemperature persists value`() = testScope.runTest {
        repository.updateColorTemperature(0.5f)
        val settings = repository.settings.first()
        assertEquals(0.5f, settings.colorTemperature, 0.001f)
    }

    @Test
    fun `updateColorTemperature coerces value to 0-1 range`() = testScope.runTest {
        repository.updateColorTemperature(1.5f)
        val settings = repository.settings.first()
        assertEquals(1.0f, settings.colorTemperature, 0.001f)

        repository.updateColorTemperature(-0.5f)
        val settingsAfter = repository.settings.first()
        assertEquals(0.0f, settingsAfter.colorTemperature, 0.001f)
    }

    @Test
    fun `updateSettings persists both brightness and color temperature`() = testScope.runTest {
        val testBrightness = 0.7f
        val testColorTemp = 0.3f
        repository.updateSettings(testBrightness, testColorTemp)
        val settings = repository.settings.first()
        assertEquals(0.7f, settings.brightness, 0.001f)
        assertEquals(0.3f, settings.colorTemperature, 0.001f)
    }

    @Test
    fun `resetToDefaults restores default values`() = testScope.runTest {
        // First change values
        repository.updateSettings(0.2f, 0.3f)

        // Then reset
        repository.resetToDefaults()

        val settings = repository.settings.first()
        assertEquals(ScreenSettings.DEFAULT_BRIGHTNESS, settings.brightness, 0.001f)
        assertEquals(ScreenSettings.DEFAULT_COLOR_TEMPERATURE, settings.colorTemperature, 0.001f)
    }
}
