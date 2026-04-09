package com.kwakwonjo.cryptoorderbook.core.domain.usecase

import com.kwakwonjo.cryptoorderbook.core.domain.repository.NetworkStatusRepository
import com.kwakwonjo.cryptoorderbook.core.model.ConnectivityStatus
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveConnectivityUseCase @Inject constructor(
    private val networkStatusRepository: NetworkStatusRepository,
) {
    operator fun invoke(): Flow<ConnectivityStatus> = networkStatusRepository.observeConnectivity()
}
