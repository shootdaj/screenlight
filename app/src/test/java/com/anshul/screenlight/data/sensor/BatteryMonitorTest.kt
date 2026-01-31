package com.anshul.screenlight.data.sensor

import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryMonitorTest {
    @Test
    fun `LOW_BATTERY_THRESHOLD is 15 percent`() {
        assertEquals(15, BatteryMonitor.LOW_BATTERY_THRESHOLD)
    }
}
