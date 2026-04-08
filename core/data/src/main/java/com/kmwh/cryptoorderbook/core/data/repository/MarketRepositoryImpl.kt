package com.kmwh.cryptoorderbook.core.data.repository

import com.kmwh.cryptoorderbook.core.domain.repository.MarketRepository
import com.kmwh.cryptoorderbook.core.model.MarketSummary
import com.kmwh.cryptoorderbook.core.network.api.UpbitApi
import javax.inject.Inject
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

class MarketRepositoryImpl @Inject constructor(
    private val upbitApi: UpbitApi,
) : MarketRepository {

    override fun observeMarketSummaries(
        pollingIntervalMillis: Long,
    ): Flow<List<MarketSummary>> = flow {
        while (currentCoroutineContext().isActive) {
            emit(fetchMarketSummaries())
            delay(pollingIntervalMillis)
        }
    }

    private suspend fun fetchMarketSummaries(): List<MarketSummary> {
        val krwMarkets = upbitApi.getMarkets()
            .filter { it.market.startsWith(KRW_MARKET_PREFIX) }

        if (krwMarkets.isEmpty()) {
            return emptyList()
        }

        val tickersByMarket = upbitApi.getTickers(
            markets = krwMarkets.joinToString(",") { it.market },
        ).associateBy { it.market }

        return krwMarkets.mapNotNull { market ->
            val ticker = tickersByMarket[market.market] ?: return@mapNotNull null
            MarketSummary(
                market = market.market,
                koreanName = market.koreanName,
                englishName = market.englishName,
                tradePrice = ticker.tradePrice,
                signedChangeRate = ticker.signedChangeRate,
            )
        }
    }

    private companion object {
        const val KRW_MARKET_PREFIX = "KRW-"
    }
}
