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

@OptIn(ExperimentalCoroutinesApi::class)
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
        assertEquals(1.0f, settings.brightness, 0.001f)
        assertEquals(ScreenSettings.DEFAULT_COLOR_ARGB, settings.colorArgb)
    }

    @Test
    fun `updateBrightness persists value`() = testScope.runTest {
        repository.updateBrightness(0.5f)
        val settings = repository.settings.first()
        assertEquals(0.5f, settings.brightness, 0.001f)
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
    fun `updateColor persists value`() = testScope.runTest {
        val testColor = 0xFFFF0000.toInt()
        repository.updateColor(testColor)
        val settings = repository.settings.first()
        assertEquals(testColor, settings.colorArgb)
    }

    @Test
    fun `updateSettings persists both brightness and color`() = testScope.runTest {
        val testSettings = ScreenSettings(brightness = 0.7f, colorArgb = 0xFF00FF00.toInt())
        repository.updateSettings(testSettings)
        val settings = repository.settings.first()
        assertEquals(0.7f, settings.brightness, 0.001f)
        assertEquals(0xFF00FF00.toInt(), settings.colorArgb)
    }
}
