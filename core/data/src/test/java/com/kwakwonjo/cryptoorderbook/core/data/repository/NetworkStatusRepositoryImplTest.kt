package com.kwakwonjo.cryptoorderbook.core.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import app.cash.turbine.test
import com.kwakwonjo.cryptoorderbook.core.model.NetworkAvailability
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkStatusRepositoryImplTest {

    @Test
    fun `observeConnectivity emits initial status immediately`() = runTest {
        // given
        val context = mockk<Context>()
        val connectivityManager = mockk<ConnectivityManager>()
        val network = mockk<Network>()
        val capabilities = connectedCapabilities()
        val callbackSlot = slot<ConnectivityManager.NetworkCallback>()

        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        every { connectivityManager.registerDefaultNetworkCallback(capture(callbackSlot)) } just runs
        every { connectivityManager.unregisterNetworkCallback(any<ConnectivityManager.NetworkCallback>()) } just runs

        val repository = NetworkStatusRepositoryImpl(context)

        repository.observeConnectivity().test {
            // when
            assertEquals(NetworkAvailability.CONNECTED, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        // then
        verify(exactly = 1) { connectivityManager.registerDefaultNetworkCallback(any<ConnectivityManager.NetworkCallback>()) }
        verify(exactly = 1) { connectivityManager.unregisterNetworkCallback(any<ConnectivityManager.NetworkCallback>()) }
    }

    @Test
    fun `observeConnectivity emits connected and disconnected transitions`() = runTest {
        // given
        val context = mockk<Context>()
        val connectivityManager = mockk<ConnectivityManager>()
        val network = mockk<Network>()
        val connectedCapabilities = connectedCapabilities()
        val disconnectedCapabilities = disconnectedCapabilities()
        val callbackSlot = slot<ConnectivityManager.NetworkCallback>()

        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { connectivityManager.activeNetwork } returns null
        every { connectivityManager.getNetworkCapabilities(any()) } returns null
        every { connectivityManager.registerDefaultNetworkCallback(capture(callbackSlot)) } just runs
        every { connectivityManager.unregisterNetworkCallback(any<ConnectivityManager.NetworkCallback>()) } just runs

        val repository = NetworkStatusRepositoryImpl(context)

        repository.observeConnectivity().test {
            // when
            assertEquals(NetworkAvailability.DISCONNECTED, awaitItem())

            callbackSlot.captured.onCapabilitiesChanged(network, connectedCapabilities)
            assertEquals(NetworkAvailability.CONNECTED, awaitItem())

            callbackSlot.captured.onCapabilitiesChanged(network, disconnectedCapabilities)
            assertEquals(NetworkAvailability.DISCONNECTED, awaitItem())

            callbackSlot.captured.onLost(network)

            // then
            expectNoEvents()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isNetworkAvailable treats transport-backed internet as connected`() {
        val context = mockk<Context>()
        val connectivityManager = mockk<ConnectivityManager>()
        val network = mockk<Network>()
        val capabilities = connectedCapabilities(validated = false, wifi = true)

        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities

        val repository = NetworkStatusRepositoryImpl(context)

        assertEquals(true, repository.isNetworkAvailable())
    }

    private fun connectedCapabilities(
        validated: Boolean = true,
        wifi: Boolean = false,
        cellular: Boolean = false,
        ethernet: Boolean = false,
        vpn: Boolean = false,
    ): NetworkCapabilities {
        val capabilities = mockk<NetworkCapabilities>()
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns validated
        every { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns wifi
        every { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns cellular
        every { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) } returns ethernet
        every { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) } returns vpn
        return capabilities
    }

    private fun disconnectedCapabilities(): NetworkCapabilities {
        val capabilities = mockk<NetworkCapabilities>()
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns false
        every { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns false
        every { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns false
        every { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) } returns false
        every { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) } returns false
        return capabilities
    }
}
