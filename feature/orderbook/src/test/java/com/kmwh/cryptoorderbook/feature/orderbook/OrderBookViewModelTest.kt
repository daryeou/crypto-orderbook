package com.kmwh.cryptoorderbook.feature.orderbook

import com.kmwh.cryptoorderbook.core.domain.repository.OrderBookRepository
import com.kmwh.cryptoorderbook.core.domain.usecase.ObserveOrderBookUseCase
import com.kmwh.cryptoorderbook.core.model.ConnectionState
import com.kmwh.cryptoorderbook.core.model.OrderBook
import com.kmwh.cryptoorderbook.core.model.OrderBookPayload
import com.kmwh.cryptoorderbook.core.model.OrderBookUnit
import com.kmwh.cryptoorderbook.core.model.TickerSnapshot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrderBookViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun `start collects orderbook and ticker into connected state`() = runTest {
        val repository = FakeOrderBookRepository(
            flow = flowOf(
                OrderBookPayload(connectionState = ConnectionState.Connecting),
                OrderBookPayload(
                    connectionState = ConnectionState.Connected,
                    orderBook = sampleOrderBook,
                ),
                OrderBookPayload(
                    connectionState = ConnectionState.Connected,
                    orderBook = sampleOrderBook,
                    ticker = TickerSnapshot(
                        market = "KRW-BTC",
                        tradePrice = 101.0,
                        signedChangeRate = 0.02,
                        timestamp = 2L,
                    ),
                ),
            )
        )
        val viewModel = OrderBookViewModel(ObserveOrderBookUseCase(repository))

        viewModel.start("KRW-BTC", "비트코인")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ConnectionState.Connected, state.connectionState)
        assertEquals(101.0, state.currentPrice)
        assertEquals(0.02, state.signedChangeRate)
        assertEquals("비트코인", state.marketLabel)
        assertNotNull(state.orderBook)
    }

    @Test
    fun `stop cancels active repository flow collection`() = runTest {
        var wasCancelled = false
        val repository = FakeOrderBookRepository(
            flow = callbackFlow {
                trySend(OrderBookPayload(connectionState = ConnectionState.Connecting))
                awaitClose {
                    wasCancelled = true
                }
            }
        )
        val viewModel = OrderBookViewModel(ObserveOrderBookUseCase(repository))

        viewModel.start("KRW-BTC", "비트코인")
        advanceUntilIdle()
        viewModel.stop()
        advanceUntilIdle()

        assertTrue(wasCancelled)
        assertEquals(ConnectionState.Connecting, viewModel.uiState.value.connectionState)
    }

    private class FakeOrderBookRepository(
        private val flow: Flow<OrderBookPayload>,
    ) : OrderBookRepository {
        override fun observeOrderBook(market: String): Flow<OrderBookPayload> = flow
    }

    private companion object {
        val sampleOrderBook = OrderBook(
            market = "KRW-BTC",
            asks = listOf(OrderBookUnit(102.0, 0.1)),
            bids = listOf(OrderBookUnit(100.0, 0.2)),
            totalAskSize = 1.0,
            totalBidSize = 2.0,
            timestamp = 1L,
        )
    }
}
