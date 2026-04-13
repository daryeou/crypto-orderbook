package com.kwakwonjo.cryptoorderbook.core.domain.usecase

import com.kwakwonjo.cryptoorderbook.core.domain.repository.MarketRepository
import com.kwakwonjo.cryptoorderbook.core.model.Ticker
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan

class GetTickerListUseCase @Inject constructor(
    private val marketRepository: MarketRepository,
) {
    operator fun invoke(markets: List<String>): Flow<List<Ticker>> = flow {
        if (markets.isEmpty()) {
            emit(emptyList())
            return@flow
        }

        val initialTickers = marketRepository.fetchTickerList(markets)
        val initialTickerMap = initialTickers.associateBy(Ticker::market)

        emit(markets.mapNotNull(initialTickerMap::get))
        emitAll(
            marketRepository.observeTickerUpdates(markets)
                .scan(initialTickerMap) { currentTickers, ticker ->
                    currentTickers + (ticker.market to ticker)
                }
                .drop(1)
                .map { tickersByMarket ->
                    markets.mapNotNull(tickersByMarket::get)
                },
        )
    }
}


