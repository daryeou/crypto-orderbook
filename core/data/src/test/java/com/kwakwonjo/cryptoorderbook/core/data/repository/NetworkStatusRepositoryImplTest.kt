package com.kwakwonjo.cryptoorderbook.core.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import app.cash.turbine.test
import com.kwakwonjo.cryptoorderbook.core.model.ConnectivityStatus
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
            assertEquals(ConnectivityStatus.CONNECTED, awaitItem())
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
            assertEquals(ConnectivityStatus.DISCONNECTED, awaitItem())

            callbackSlot.captured.onCapabilitiesChanged(network, connectedCapabilities)
            assertEquals(ConnectivityStatus.CONNECTED, awaitItem())

            callbackSlot.captured.onCapabilitiesChanged(network, disconnectedCapabilities)
            assertEquals(ConnectivityStatus.DISCONNECTED, awaitItem())

            callbackSlot.captured.onLost(network)

            // then
            expectNoEvents()

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun connectedCapabilities(): NetworkCapabilities {
        val capabilities = mockk<NetworkCapabilities>()
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true
        return capabilities
    }

    private fun disconnectedCapabilities(): NetworkCapabilities {
        val capabilities = mockk<NetworkCapabilities>()
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns false
        return capabilities
    }
}
