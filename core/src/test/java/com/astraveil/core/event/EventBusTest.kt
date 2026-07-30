package com.astraveil.core.event

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [EventBus] subscription and delivery semantics.
 *
 * The bus is backed by a `MutableSharedFlow` with `replay = 0` and a
 * `DROP_OLDEST` buffer of 64 — these tests pin the baseline guarantees:
 *
 *  - subscribers see only events emitted AFTER they subscribe;
 *  - `tryEmit` returns `true` whenever buffer capacity is available;
 *  - events are delivered in FIFO order;
 *  - [EventBus.eventsOf] filters by runtime event type;
 *  - `emit` without any subscriber is a safe no-op.
 *
 * Run on the JVM via `kotlinx-coroutines-test` — no Android dependency.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EventBusTest {

    @Test
    fun `subscribe_receives_event`() = runTest {
        val received = mutableListOf<AstraEvent>()
        val job = launch {
            EventBus.events.collect { received.add(it) }
        }
        // Let the collector register as a subscriber before emitting.
        runCurrent()

        EventBus.emit(PermissionGrantedEvent(moduleId = "mod.a", permission = "filesystem"))
        runCurrent()

        job.cancel()
        assertEquals(1, received.size)
        val ev = received[0]
        assertTrue("expected PermissionGrantedEvent", ev is PermissionGrantedEvent)
        ev as PermissionGrantedEvent
        assertEquals("mod.a", ev.moduleId)
        assertEquals("filesystem", ev.permission)
    }

    @Test
    fun `tryEmit_returns_true`() = runTest {
        val result = EventBus.tryEmit(PermissionGrantedEvent(moduleId = "mod.b", permission = "mount"))
        assertTrue("tryEmit with available buffer capacity must return true", result)
    }

    @Test
    fun `multiple_events_delivered_in_order`() = runTest {
        val received = mutableListOf<AstraEvent>()
        val job = launch {
            EventBus.events.collect { received.add(it) }
        }
        runCurrent()

        EventBus.emit(PermissionGrantedEvent("mod.a", "p1"))
        EventBus.emit(PermissionGrantedEvent("mod.a", "p2"))
        EventBus.emit(PermissionGrantedEvent("mod.a", "p3"))
        runCurrent()

        job.cancel()
        assertEquals(3, received.size)
        assertEquals("p1", (received[0] as PermissionGrantedEvent).permission)
        assertEquals("p2", (received[1] as PermissionGrantedEvent).permission)
        assertEquals("p3", (received[2] as PermissionGrantedEvent).permission)
    }

    @Test
    fun `eventsOf_filters_by_type`() = runTest {
        val received = mutableListOf<PermissionGrantedEvent>()
        val job = launch {
            EventBus.eventsOf<PermissionGrantedEvent>().collect { received.add(it) }
        }
        runCurrent()

        EventBus.emit(PermissionGrantedEvent(moduleId = "mod.a", permission = "grant"))
        EventBus.emit(ModuleInstalledEvent(moduleId = "mod.x", version = "1.0.0"))
        runCurrent()

        job.cancel()
        assertEquals(1, received.size)
        assertEquals("mod.a", received[0].moduleId)
        assertEquals("grant", received[0].permission)
    }

    @Test
    fun `emit_without_subscribers_is_noop`() = runTest {
        // No collector is launched. emit() must not throw and must
        // simply drop the value (replay=0, no subscribers).
        EventBus.emit(PermissionGrantedEvent(moduleId = "mod.c", permission = "network"))
        // If we reach this assertion, emit did not throw.
        // Sanity check: subscribing AFTER the emit must NOT see the event
        // (replay=0 ⇒ no replay of past emissions).
        val seen = mutableListOf<AstraEvent>()
        val job = launch {
            EventBus.events.collect { seen.add(it) }
        }
        runCurrent()
        job.cancel()
        assertTrue("late subscriber must not see prior emit", seen.isEmpty())
    }
}
