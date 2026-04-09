package com.kwakwonjo.cryptoorderbook.core.domain.repository

import com.kwakwonjo.cryptoorderbook.core.model.ConnectivityStatus
import kotlinx.coroutines.flow.Flow

interface NetworkStatusRepository {
    fun observeConnectivity(): Flow<ConnectivityStatus>

    fun isNetworkAvailable(): Boolean
}
