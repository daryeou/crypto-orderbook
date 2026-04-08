package com.kwakwonjo.cryptoorderbook.core.domain.usecase

import com.kwakwonjo.cryptoorderbook.core.domain.repository.NetworkStatusRepository
import javax.inject.Inject

class IsNetworkAvailableUseCase @Inject constructor(
    private val networkStatusRepository: NetworkStatusRepository,
) {
    operator fun invoke(): Boolean = networkStatusRepository.isNetworkAvailable()
}
