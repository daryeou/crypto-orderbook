package com.kwakwonjo.cryptoorderbook.core.domain.usecase

import com.kwakwonjo.cryptoorderbook.core.domain.repository.MarketRepository
import com.kwakwonjo.cryptoorderbook.core.model.MarketSummary
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveMarketSummariesUseCase @Inject constructor(
    private val marketRepository: MarketRepository,
) {
    operator fun invoke(
        pollingIntervalMillis: Long = DEFAULT_POLLING_INTERVAL_MILLIS,
    ): Flow<List<MarketSummary>> {
        return marketRepository.observeMarketSummaries(pollingIntervalMillis)
    }

    private companion object {
        const val DEFAULT_POLLING_INTERVAL_MILLIS = 5_000L
    }
}


