package com.astraveil.core.update.github

import org.junit.Assert.*
import org.junit.Test

class VersionComparatorTest {

    @Test
    fun `semantic version comparison`() {
        assertTrue(VersionComparator.isNewer("0.2.0", "0.1.0"))
        assertTrue(VersionComparator.isNewer("1.0.0", "0.9.9"))
        assertFalse(VersionComparator.isNewer("0.1.0", "0.1.0"))
        assertFalse(VersionComparator.isNewer("0.1.0", "0.2.0"))
    }

    @Test
    fun `handles v prefix`() {
        assertTrue(VersionComparator.isNewer("v0.2.0", "v0.1.0"))
        assertTrue(VersionComparator.isNewer("v1.0.0", "0.9.0"))
    }

    @Test
    fun `handles alpha suffix`() {
        // 0.1.0 > 0.1.0-alpha13
        assertTrue(VersionComparator.isNewer("0.1.0", "0.1.0-alpha13"))
        // 0.1.0-alpha14 > 0.1.0-alpha13
        assertTrue(VersionComparator.isNewer("0.1.0-alpha14", "0.1.0-alpha13"))
    }

    @Test
    fun `handles malformed input gracefully`() {
        assertFalse(VersionComparator.isNewer("", "0.1.0"))
        assertFalse(VersionComparator.isNewer("abc", "0.1.0"))
        assertFalse(VersionComparator.isNewer("0.1.0", ""))
    }
}
