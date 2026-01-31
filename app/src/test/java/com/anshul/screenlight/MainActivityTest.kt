package com.anshul.screenlight

import android.os.Build
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class MainActivityTest {
    @Test
    fun activityCreatesSuccessfully() {
        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .get()
        assertNotNull("MainActivity should be created", activity)
    }

    @Test
    fun activityResumesSuccessfully() {
        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .start()
            .resume()
            .get()
        assertNotNull("MainActivity should resume", activity)
    }

    @Test
    fun activityPausesSuccessfully() {
        val controller = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .start()
            .resume()
        val activity = controller.pause().get()
        assertNotNull("MainActivity should pause", activity)
    }
}
