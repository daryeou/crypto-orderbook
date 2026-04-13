package com.kwakwonjo.cryptoorderbook.core.data.repository

import app.cash.turbine.test
import com.kwakwonjo.cryptoorderbook.core.model.ConnectionState
import com.kwakwonjo.cryptoorderbook.core.network.model.UpbitOrderBookFrame
import com.kwakwonjo.cryptoorderbook.core.network.model.UpbitOrderBookUnitFrame
import com.kwakwonjo.cryptoorderbook.core.network.model.UpbitSubscription
import com.kwakwonjo.cryptoorderbook.core.network.model.UpbitTickerFrame
import com.kwakwonjo.cryptoorderbook.core.network.model.UpbitWsFrame
import com.kwakwonjo.cryptoorderbook.core.network.websocket.UpbitWebSocketClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrderBookRepositoryImplTest {

    @Test
    fun `repository merges orderbook and ticker frames into latest payload`() = runTest {
        val repository = OrderBookRepositoryImpl(
            upbitWebSocketClient = FakeUpbitWebSocketClient(
                flow = flow {
                    emit(
                        UpbitOrderBookFrame(
                            market = "KRW-BTC",
                            totalAskSize = 3.0,
                            totalBidSize = 2.0,
                            units = listOf(
                                UpbitOrderBookUnitFrame(
                                    askPrice = 102.0,
                                    bidPrice = 100.0,
                                    askSize = 0.1,
                                    bidSize = 0.2,
                                )
                            ),
                            timestamp = 1L,
                        )
                    )
                    emit(
                        UpbitTickerFrame(
                            market = "KRW-BTC",
                            tradePrice = 101.0,
                            signedChangeRate = 0.02,
                            timestamp = 2L,
                        )
                    )
                }
            )
        )

        repository.observeOrderBook("KRW-BTC", 15).test {
            val initial = awaitItem()
            assertEquals(ConnectionState.Connecting, initial.connectionState)
            assertNull(initial.orderBook)

            val afterOrderBook = awaitItem()
            assertEquals(ConnectionState.Connected, afterOrderBook.connectionState)
            assertEquals(102.0, afterOrderBook.orderBook?.asks?.first()?.price)

            val afterTicker = awaitItem()
            assertEquals(101.0, afterTicker.ticker?.tradePrice)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `repository emits error state when websocket flow fails`() = runTest {
        val repository = OrderBookRepositoryImpl(
            upbitWebSocketClient = FakeUpbitWebSocketClient(
                flow = flow {
                    error("socket failed")
                }
            )
        )

        repository.observeOrderBook("KRW-BTC", 15).test {
            assertEquals(ConnectionState.Connecting, awaitItem().connectionState)
            val error = awaitItem()
            assertEquals(ConnectionState.Error, error.connectionState)
            assertEquals("socket failed", error.errorMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `repository retries io failures before emitting error state`() = runTest {
        var collectCount = 0
        val repository = OrderBookRepositoryImpl(
            upbitWebSocketClient = FakeUpbitWebSocketClient(
                flow = flow {
                    collectCount += 1
                    emit(
                        UpbitOrderBookFrame(
                            market = "KRW-BTC",
                            totalAskSize = 3.0,
                            totalBidSize = 2.0,
                            units = listOf(
                                UpbitOrderBookUnitFrame(
                                    askPrice = 102.0,
                                    bidPrice = 100.0,
                                    askSize = 0.1,
                                    bidSize = 0.2,
                                )
                            ),
                            timestamp = collectCount.toLong(),
                        )
                    )
                    throw IOException("socket failed $collectCount")
                }
            )
        )

        repository.observeOrderBook("KRW-BTC", 15).test {
            assertEquals(ConnectionState.Connecting, awaitItem().connectionState)
            repeat(4) { attempt ->
                val latest = awaitItem()
                assertEquals(ConnectionState.Connected, latest.connectionState)
                assertEquals((attempt + 1).toLong(), latest.orderBook?.timestamp)
            }

            val error = awaitItem()
            assertEquals(ConnectionState.Error, error.connectionState)
            assertEquals("socket failed 4", error.errorMessage)
            assertEquals(4, collectCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private class FakeUpbitWebSocketClient(
        private val flow: Flow<UpbitWsFrame>,
    ) : UpbitWebSocketClient {
        override fun observeUpbitStream(subscription: UpbitSubscription): Flow<UpbitWsFrame> = flow

        override fun observeTickerStream(markets: List<String>): Flow<UpbitTickerFrame> = emptyFlow()
    }
}

