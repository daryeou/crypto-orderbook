package com.kwakwonjo.cryptoorderbook.core.data.repository

import app.cash.turbine.test
import com.kwakwonjo.cryptoorderbook.core.model.Ticker
import com.kwakwonjo.cryptoorderbook.core.network.api.UpbitApi
import com.kwakwonjo.cryptoorderbook.core.network.model.UpbitMarketResponse
import com.kwakwonjo.cryptoorderbook.core.network.model.UpbitSubscription
import com.kwakwonjo.cryptoorderbook.core.network.model.UpbitTickerFrame
import com.kwakwonjo.cryptoorderbook.core.network.model.UpbitTickerResponse
import com.kwakwonjo.cryptoorderbook.core.network.model.UpbitWsFrame
import com.kwakwonjo.cryptoorderbook.core.network.websocket.UpbitWebSocketClient
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MarketRepositoryImplTest {

    @Test
    fun `observeTickerUpdates maps websocket ticker frames to domain ticker`() = runTest {
        val webSocketClient = FakeUpbitWebSocketClient(
            tickerFlow = flowOf(
                UpbitTickerFrame(
                    market = "KRW-BTC",
                    tradePrice = 148_956_000.0,
                    signedChangeRate = 0.0031,
                    timestamp = 1L,
                )
            )
        )
        val repository = MarketRepositoryImpl(
            upbitApi = FakeUpbitApi(),
            upbitWebSocketClient = webSocketClient,
        )

        repository.observeTickerUpdates(listOf("KRW-BTC")).test {
            assertEquals(
                Ticker(
                    market = "KRW-BTC",
                    tradePrice = 148_956_000.0,
                    signedChangeRate = 0.0031,
                ),
                awaitItem(),
            )
            assertEquals(listOf(listOf("KRW-BTC")), webSocketClient.observeTickerRequests)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeTickerUpdates retries io failures up to three times`() = runTest {
        var attempts = 0
        val webSocketClient = FakeUpbitWebSocketClient(
            tickerFlow = flow {
                attempts += 1
                throw IOException("socket failed $attempts")
            }
        )
        val repository = MarketRepositoryImpl(
            upbitApi = FakeUpbitApi(),
            upbitWebSocketClient = webSocketClient,
        )

        repository.observeTickerUpdates(listOf("KRW-BTC")).test {
            advanceUntilIdle()
            val error = awaitError()
            assertEquals("socket failed 4", error.message)
            assertEquals(4, attempts)
        }
    }

    private class FakeUpbitApi : UpbitApi {
        override suspend fun getMarkets(isDetails: Boolean): List<UpbitMarketResponse> = emptyList()

        override suspend fun getTickers(markets: String): List<UpbitTickerResponse> = emptyList()
    }

    private class FakeUpbitWebSocketClient(
        private val tickerFlow: Flow<UpbitTickerFrame>,
    ) : UpbitWebSocketClient {
        val observeTickerRequests = mutableListOf<List<String>>()

        override fun observeUpbitStream(subscription: UpbitSubscription): Flow<UpbitWsFrame> {
            return emptyFlow()
        }

        override fun observeTickerStream(markets: List<String>): Flow<UpbitTickerFrame> {
            observeTickerRequests += markets
            return tickerFlow
        }
    }
}
