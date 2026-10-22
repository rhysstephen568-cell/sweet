package com.sweet.app.core.network.nearby

import kotlin.math.abs

enum class ProximityTier(val label: String, val maxRttMs: Long) {
    VERY_CLOSE("Very Close (<15ms)", 15L),
    CLOSE("Close (<40ms)", 40L),
    FAR("Far (<100ms)", 100L),
    DISTANT("Distant (>100ms)", Long.MAX_VALUE);

    companion object {
        fun fromRtt(rttMs: Long): ProximityTier {
            return when {
                rttMs < 15L -> VERY_CLOSE
                rttMs < 40L -> CLOSE
                rttMs < 100L -> FAR
                else -> DISTANT
            }
        }
    }
}

data class NearbyPeer(
    val userId: String,
    val name: String,
    val avatarUrl: String? = null,
    val host: String,
    val port: Int,
    val rttMs: Long,
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val signalLevel: Int = -50,
    val isBlocked: Boolean = false
) {
    val proximityTier: ProximityTier get() = ProximityTier.fromRtt(rttMs)

    // Stable angle calculation derived from userId hash to prevent node jumping
    val stableAngleRad: Float
        get() {
            val hash = abs(userId.hashCode())
            val baseAngle = (hash % 360).toFloat()
            return Math.toRadians(baseAngle.toDouble()).toFloat()
        }
}
