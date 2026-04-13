package com.kwakwonjo.cryptoorderbook.core.data.repository

import com.kwakwonjo.cryptoorderbook.core.data.mapper.toDomain
import com.kwakwonjo.cryptoorderbook.core.data.mapper.toTicker
import com.kwakwonjo.cryptoorderbook.core.domain.repository.MarketRepository
import com.kwakwonjo.cryptoorderbook.core.model.Market
import com.kwakwonjo.cryptoorderbook.core.model.Ticker
import com.kwakwonjo.cryptoorderbook.core.network.api.UpbitApi
import com.kwakwonjo.cryptoorderbook.core.network.websocket.UpbitWebSocketClient
import javax.inject.Inject
import java.io.IOException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen

class MarketRepositoryImpl @Inject constructor(
    private val upbitApi: UpbitApi,
    private val upbitWebSocketClient: UpbitWebSocketClient,
) : MarketRepository {

    override suspend fun fetchMarketList(): List<Market> {
        return upbitApi.getMarkets().map {
            it.toDomain()
        }
    }

    override suspend fun fetchTickerList(markets: List<String>): List<Ticker> {
        return upbitApi.getTickers(
            markets = markets.joinToString(",") { it },
        ).map {
            it.toDomain()
        }
    }

    override fun observeTickerUpdates(markets: List<String>): Flow<Ticker> {
        if (markets.isEmpty()) {
            return emptyFlow()
        }

        return upbitWebSocketClient.observeTickerStream(markets)
            .map { it.toTicker() }
            .retryWhen { cause, attempt ->
                if (cause is IOException && attempt < 3) {
                    val delayTime = minOf(1_000L * (attempt + 1), 10_000L)
                    delay(delayTime)
                    true
                } else {
                    false
                }
            }
    }
}

