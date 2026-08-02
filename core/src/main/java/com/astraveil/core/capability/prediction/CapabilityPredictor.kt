package com.astraveil.core.capability.prediction

import com.astraveil.core.logger.AstraLogger
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap

/**
 * Predictive Capability Degradation Engine.
 *
 * Monitors the historical availability of each capability and uses
 * Exponentially Weighted Moving Average (EWMA) to predict whether
 * a capability is trending toward unavailability.
 *
 * Analogous to TCP congestion control's RTT estimation: instead of
 * waiting for a packet loss (capability failure), we detect the TREND
 * and warn modules to degrade proactively.
 *
 * Algorithm:
 * 1. Each capability probe result (true/false) is fed into an EWMA.
 * 2. EWMA_t = α * observation + (1-α) * EWMA_{t-1}
 * 3. If EWMA drops below [degradationThreshold], the capability is
 *    flagged as "degrading".
 * 4. If EWMA drops below [failureThreshold], the capability is
 *    flagged as "predicted_failure".
 */
class CapabilityPredictor(
    private val alpha: Double = 0.3,
    private val degradationThreshold: Double = 0.7,
    private val failureThreshold: Double = 0.3,
) {
    @Serializable
    data class PredictionState(
        val capability: String,
        val ewma: Double,
        val sampleCount: Int,
        val lastObservation: Boolean,
        val lastUpdateMs: Long,
        val status: CapabilityStatus,
        val trend: Trend,
    )

    enum class CapabilityStatus {
        HEALTHY,
        DEGRADING,
        PREDICTED_FAILURE,
        INSUFFICIENT_DATA,
    }

    enum class Trend {
        IMPROVING,
        STABLE,
        DECLINING,
    }

    private data class EwmaState(
        var ewma: Double = -1.0,
        var prevEwma: Double = -1.0,
        var sampleCount: Int = 0,
        var lastObservation: Boolean = false,
        var lastUpdateMs: Long = 0,
    )

    private val states = ConcurrentHashMap<String, EwmaState>()

    fun observe(capability: String, available: Boolean): PredictionState {
        val state = states.getOrPut(capability) { EwmaState() }
        val observation = if (available) 1.0 else 0.0

        state.prevEwma = state.ewma
        state.ewma = if (state.ewma < 0) {
            observation
        } else {
            alpha * observation + (1.0 - alpha) * state.ewma
        }
        state.sampleCount++
        state.lastObservation = available
        state.lastUpdateMs = System.currentTimeMillis()

        val prediction = toPrediction(capability, state)

        if (prediction.status == CapabilityStatus.PREDICTED_FAILURE) {
            AstraLogger.w(TAG, "PREDICTED FAILURE: '$capability' " +
                "EWMA=${"%.3f".format(state.ewma)} " +
                "(samples=${state.sampleCount})")
        } else if (prediction.status == CapabilityStatus.DEGRADING) {
            AstraLogger.i(TAG, "DEGRADING: '$capability' " +
                "EWMA=${"%.3f".format(state.ewma)}")
        }

        return prediction
    }

    fun predict(capability: String): PredictionState? {
        val state = states[capability] ?: return null
        return toPrediction(capability, state)
    }

    fun allPredictions(): List<PredictionState> {
        return states.map { (cap, state) -> toPrediction(cap, state) }
            .sortedBy { it.ewma }
    }

    fun reset() {
        states.clear()
    }

    private fun toPrediction(capability: String, state: EwmaState): PredictionState {
        val status = when {
            state.sampleCount < MIN_SAMPLES -> CapabilityStatus.INSUFFICIENT_DATA
            state.ewma >= degradationThreshold -> CapabilityStatus.HEALTHY
            state.ewma >= failureThreshold -> CapabilityStatus.DEGRADING
            else -> CapabilityStatus.PREDICTED_FAILURE
        }

        val trend = when {
            state.prevEwma < 0 -> Trend.STABLE
            state.ewma > state.prevEwma + TREND_EPSILON -> Trend.IMPROVING
            state.ewma < state.prevEwma - TREND_EPSILON -> Trend.DECLINING
            else -> Trend.STABLE
        }

        return PredictionState(
            capability = capability,
            ewma = state.ewma,
            sampleCount = state.sampleCount,
            lastObservation = state.lastObservation,
            lastUpdateMs = state.lastUpdateMs,
            status = status,
            trend = trend,
        )
    }

    companion object {
        private const val TAG = "CapabilityPredictor"
        private const val MIN_SAMPLES = 5
        private const val TREND_EPSILON = 0.01
    }
}
