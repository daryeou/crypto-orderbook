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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

    /**
     * `markets` 쿼리 파라미터에 너무 많은 종목을 한 번에 묶으면
     * URL 길이 제한(약 4KB)을 초과하여 HTTP 400 Bad Request를 반환한다.
     * 따라서 [TICKER_CHUNK_SIZE] 단위로 끊어 병렬 호출한 뒤 결과를 합쳐서 반환한다.
     */
    override suspend fun fetchTickerList(markets: List<String>): List<Ticker> = coroutineScope {
        if (markets.isEmpty()) return@coroutineScope emptyList()

        markets.chunked(TICKER_CHUNK_SIZE)
            .map { chunk ->
                async {
                    upbitApi.getTickers(
                        markets = chunk.joinToString(","),
                    ).map { it.toDomain() }
                }
            }
            .awaitAll()
            .flatten()
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

    private companion object {
        const val TICKER_CHUNK_SIZE = 100
    }
}