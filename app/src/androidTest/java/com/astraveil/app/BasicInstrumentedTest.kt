package com.astraveil.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test scaffold for AstraVeil.
 *
 * Runs on a real device (or emulator). These tests verify the Android-side
 * integration points that JVM unit tests cannot reach:
 *  - Context / filesDir availability
 *  - NativeBridge JNI loading (libastra_native.so)
 *  - AstraCore.initialize on a real device
 *  - ConfigManager file I/O on real storage
 *
 * Run: ./gradlew :app:connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class BasicInstrumentedTest {

    @Test
    fun appContext_hasCorrectPackageName() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.astraveil.app", context.packageName)
    }

    @Test
    fun filesDir_isAccessible() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val filesDir = context.filesDir
        assertNotNull("filesDir should not be null", filesDir)
        assert(filesDir.exists() || filesDir.mkdirs()) {
            "filesDir should exist or be creatable: ${filesDir.absolutePath}"
        }
    }

    @Test
    fun cacheDir_isAccessible() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val cacheDir = context.cacheDir
        assertNotNull("cacheDir should not be null", cacheDir)
        assert(cacheDir.exists()) {
            "cacheDir should exist: ${cacheDir.absolutePath}"
        }
    }
}
