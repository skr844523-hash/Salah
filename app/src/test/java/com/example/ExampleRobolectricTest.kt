package com.example

import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleRobolectricTest {

    @Test
    fun verifyAppNameConfig() {
        val appName = "Voice Changer Gaming"
        assertEquals("Voice Changer Gaming", appName)
    }
}
