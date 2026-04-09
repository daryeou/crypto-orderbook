package com.kwakwonjo.cryptoorderbook.core.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.kwakwonjo.cryptoorderbook.core.domain.repository.NetworkStatusRepository
import com.kwakwonjo.cryptoorderbook.core.model.NetworkAvailability
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

class NetworkStatusRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : NetworkStatusRepository {

    override fun observeConnectivity(): Flow<NetworkAvailability> = callbackFlow {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: run {
            trySend(NetworkAvailability.DISCONNECTED)
                close()
                return@callbackFlow
            }

        trySend(connectivityManager.currentNetworkAvailability())

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(connectivityManager.currentNetworkAvailability())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                trySend(networkCapabilities.toNetworkAvailability())
            }

            override fun onLost(network: Network) {
                trySend(NetworkAvailability.DISCONNECTED)
            }

            override fun onUnavailable() {
                trySend(NetworkAvailability.DISCONNECTED)
            }
        }

        connectivityManager.registerDefaultNetworkCallback(callback)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged().conflate()

    override fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

        // NET_CAPABILITY_INTERNET: 인터넷 통신용으로 구성된 네트워크인지 확인한다.
        // NET_CAPABILITY_VALIDATED: 외부 인터넷 연결이 실제로 검증된 상태인지 확인한다.
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun ConnectivityManager.currentNetworkAvailability(): NetworkAvailability {
        val activeNetwork = activeNetwork ?: return NetworkAvailability.DISCONNECTED
        val capabilities = getNetworkCapabilities(activeNetwork) ?: return NetworkAvailability.DISCONNECTED
        return capabilities.toNetworkAvailability()
    }

    private fun NetworkCapabilities.toNetworkAvailability(): NetworkAvailability {
        return if (
            hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        ) {
            NetworkAvailability.CONNECTED
        } else {
            NetworkAvailability.DISCONNECTED
        }
    }
}
