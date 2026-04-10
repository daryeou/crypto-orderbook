package com.kwakwonjo.cryptoorderbook.core.domain.usecase

import com.kwakwonjo.cryptoorderbook.core.domain.repository.MarketRepository
import com.kwakwonjo.cryptoorderbook.core.model.Market
import com.kwakwonjo.cryptoorderbook.core.model.Ticker
import javax.inject.Inject
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

class GetTickerListUseCase @Inject constructor(
    private val marketRepository: MarketRepository,
) {
    operator fun invoke(
        markets: List<String>,
        pollingIntervalMillis: Long = DEFAULT_POLLING_INTERVAL_MILLIS,
    ): Flow<List<Ticker>> = flow {
        while (currentCoroutineContext().isActive) {
            emit(marketRepository.fetchTickerList(markets))
            delay(pollingIntervalMillis)
        }
    }

    private companion object {
        const val DEFAULT_POLLING_INTERVAL_MILLIS = 1_000L
    }
}


