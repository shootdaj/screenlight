package com.anshul.screenlight.data.sensor

import org.junit.Assert.assertEquals
import org.junit.Test

class AmbientLightManagerTest {
    @Test
    fun `DARK_THRESHOLD_LUX is 50`() {
        assertEquals(50f, AmbientLightManager.DARK_THRESHOLD_LUX, 0.001f)
    }
}
