package com.sweet.app

import com.sweet.app.core.network.nearby.NearbyPeer
import com.sweet.app.core.network.nearby.ProximityTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NearbyPeerUnitTest {

    @Test
    fun testProximityTierMapping() {
        assertEquals(ProximityTier.VERY_CLOSE, ProximityTier.fromRtt(10L))
        assertEquals(ProximityTier.CLOSE, ProximityTier.fromRtt(25L))
        assertEquals(ProximityTier.FAR, ProximityTier.fromRtt(75L))
        assertEquals(ProximityTier.DISTANT, ProximityTier.fromRtt(150L))
    }

    @Test
    fun testStableAngleCalculation() {
        val peerA = NearbyPeer(
            userId = "user_alpha_123",
            name = "Alpha",
            host = "192.168.1.10",
            port = 8088,
            rttMs = 12L
        )

        val peerA2 = NearbyPeer(
            userId = "user_alpha_123",
            name = "Alpha Updated Name",
            host = "192.168.1.10",
            port = 8088,
            rttMs = 50L
        )

        // Verify that angle remains identical regardless of latency or name updates
        assertEquals(peerA.stableAngleRad, peerA2.stableAngleRad, 0.0001f)
    }

    @Test
    fun testPeerExpiryFilter() {
        val now = System.currentTimeMillis()
        val activePeer = NearbyPeer("usr_1", "Active", host = "192.168.1.1", port = 8088, rttMs = 10L, lastSeenTimestamp = now - 5000)
        val expiredPeer = NearbyPeer("usr_2", "Expired", host = "192.168.1.2", port = 8088, rttMs = 10L, lastSeenTimestamp = now - 25000)

        val peers = listOf(activePeer, expiredPeer)
        val filteredPeers = peers.filter { now - it.lastSeenTimestamp < 20000 }

        assertEquals(1, filteredPeers.size)
        assertEquals("usr_1", filteredPeers.first().userId)
    }
}
