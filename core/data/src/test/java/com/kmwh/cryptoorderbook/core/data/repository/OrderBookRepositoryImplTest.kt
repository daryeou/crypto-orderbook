package com.kmwh.cryptoorderbook.core.data.repository

import app.cash.turbine.test
import com.kmwh.cryptoorderbook.core.model.ConnectionState
import com.kmwh.cryptoorderbook.core.network.model.UpbitOrderBookFrame
import com.kmwh.cryptoorderbook.core.network.model.UpbitOrderBookUnitFrame
import com.kmwh.cryptoorderbook.core.network.model.UpbitSubscription
import com.kmwh.cryptoorderbook.core.network.model.UpbitTickerFrame
import com.kmwh.cryptoorderbook.core.network.model.UpbitWsFrame
import com.kmwh.cryptoorderbook.core.network.websocket.UpbitWebSocketClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
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

        repository.observeOrderBook("KRW-BTC").test {
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

        repository.observeOrderBook("KRW-BTC").test {
            assertEquals(ConnectionState.Connecting, awaitItem().connectionState)
            val error = awaitItem()
            assertEquals(ConnectionState.Error, error.connectionState)
            assertEquals("socket failed", error.errorMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private class FakeUpbitWebSocketClient(
        private val flow: Flow<UpbitWsFrame>,
    ) : UpbitWebSocketClient {
        override fun observeUpbitStream(subscription: UpbitSubscription): Flow<UpbitWsFrame> = flow
    }
}
