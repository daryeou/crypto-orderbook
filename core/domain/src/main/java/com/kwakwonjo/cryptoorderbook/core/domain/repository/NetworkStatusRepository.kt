package com.kwakwonjo.cryptoorderbook.core.domain.repository

import com.kwakwonjo.cryptoorderbook.core.model.NetworkAvailability
import kotlinx.coroutines.flow.Flow

interface NetworkStatusRepository {
    fun observeConnectivity(): Flow<NetworkAvailability>

    fun isNetworkAvailable(): Boolean
}
