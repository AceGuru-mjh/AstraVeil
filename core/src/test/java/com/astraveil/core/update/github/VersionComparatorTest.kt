package com.astraveil.core.update.github

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [VersionComparator].
 *
 * The comparator exposes a single public surface —
 * `VersionComparator.isRemoteNewer(current, remote)` — which returns `true`
 * when the `remote` version string denotes a strictly newer release than
 * `current`.
 *
 * Normalization rules (see [VersionComparator] source):
 *   - Trim leading/trailing whitespace.
 *   - Strip a single leading `v` or `V` prefix.
 *   - Cut at the first character that is neither a digit nor a dot, so
 *     `"v0.1.0-alpha13"` collapses to `"0.1.0"`.
 *   - After normalization, equal inputs always yield `false`.
 *
 * These tests replace the original broken suite that called the
 * non-existent `VersionComparator.isNewer(remote, current)` overload.
 */
class VersionComparatorTest {

    @Test
    fun higher_minor_returns_true() {
        assertTrue(VersionComparator.isRemoteNewer("0.1.0", "0.2.0"))
    }

    @Test
    fun same_version_returns_false() {
        assertFalse(VersionComparator.isRemoteNewer("0.1.0", "0.1.0"))
    }

    @Test
    fun lower_version_returns_false() {
        assertFalse(VersionComparator.isRemoteNewer("0.2.0", "0.1.0"))
    }

    @Test
    fun major_bump_returns_true() {
        assertTrue(VersionComparator.isRemoteNewer("1.5.3", "2.0.0"))
    }

    @Test
    fun v_prefix_stripped_correctly() {
        // Both versions carry a `v` prefix; the prefix is stripped before
        // comparison so the numeric portions decide.
        assertTrue(VersionComparator.isRemoteNewer("v0.1.0", "v0.2.0"))
        assertFalse(VersionComparator.isRemoteNewer("v0.2.0", "v0.1.0"))
    }

    @Test
    fun mixed_v_prefix_compared_correctly() {
        // Only one side carries a `v` prefix. After normalization both
        // versions are pure numeric strings.
        assertTrue(VersionComparator.isRemoteNewer("v0.1.0", "0.2.0"))
        assertFalse(VersionComparator.isRemoteNewer("0.2.0", "v0.1.0"))
    }

    @Test
    fun alpha_suffix_stripped_to_equal() {
        // `normalize()` cuts at the first non-numeric/dot char, so the
        // "-alpha13" suffix on current is dropped. Both versions become
        // "0.1.0" and the comparator returns false (no update available).
        assertFalse(VersionComparator.isRemoteNewer("0.1.0-alpha13", "0.1.0"))
    }

    @Test
    fun patch_comparison_higher_returns_true() {
        assertTrue(VersionComparator.isRemoteNewer("1.2.3", "1.2.4"))
    }

    @Test
    fun patch_comparison_lower_returns_false() {
        assertFalse(VersionComparator.isRemoteNewer("1.2.4", "1.2.3"))
    }

    @Test
    fun empty_remote_returns_false() {
        // Remote normalizes to "" — split yields [""]; each part is missing
        // or 0, never strictly greater than the current numeric components.
        assertFalse(VersionComparator.isRemoteNewer("0.1.0", ""))
    }

    @Test
    fun empty_local_with_nonempty_remote_returns_true() {
        // Current normalizes to "" — equivalent to "0.0.0". A non-empty
        // remote like "0.1.0" therefore wins on the second numeric segment.
        assertTrue(VersionComparator.isRemoteNewer("", "0.1.0"))
    }

    @Test
    fun both_empty_returns_false() {
        // After normalization both sides are "" — equal inputs short-circuit
        // to false before any numeric comparison runs.
        assertFalse(VersionComparator.isRemoteNewer("", ""))
    }

    @Test
    fun garbage_input_does_not_crash() {
        // Pure-alphabetic inputs contain no numeric/dot characters, so
        // normalize yields "". The comparator must never throw on garbage
        // input — it returns a deterministic boolean instead.
        //
        // Both sides garbage → both normalize to "" → equal → false.
        assertFalse(VersionComparator.isRemoteNewer("abc", "xyz"))
        // Garbage current vs valid remote: "abc" normalizes to "" which
        // behaves as version 0; "0.1.0" therefore wins on the minor
        // segment → true.
        assertTrue(VersionComparator.isRemoteNewer("abc", "0.1.0"))
        // Garbage remote vs valid current: "0.1.0" beats "" → false
        // (remote is NOT newer).
        assertFalse(VersionComparator.isRemoteNewer("0.1.0", "abc"))
    }

    @Test
    fun two_part_vs_three_part_higher_patch_returns_true() {
        // A two-segment version is treated as if a missing third segment
        // were 0. Here remote "1.2.1" beats current "1.2".
        assertTrue(VersionComparator.isRemoteNewer("1.2", "1.2.1"))
    }

    @Test
    fun two_part_vs_three_part_equal_returns_false() {
        // Missing trailing segments are zero-padded; "1.2" == "1.2.0".
        assertFalse(VersionComparator.isRemoteNewer("1.2", "1.2.0"))
    }
}
