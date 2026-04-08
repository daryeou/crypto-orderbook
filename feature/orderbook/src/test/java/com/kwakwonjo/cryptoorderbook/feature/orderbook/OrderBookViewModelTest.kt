package com.kwakwonjo.cryptoorderbook.feature.orderbook

import com.kwakwonjo.cryptoorderbook.core.domain.repository.OrderBookRepository
import com.kwakwonjo.cryptoorderbook.core.domain.usecase.ObserveOrderBookUseCase
import com.kwakwonjo.cryptoorderbook.core.model.ConnectionState
import com.kwakwonjo.cryptoorderbook.core.model.OrderBook
import com.kwakwonjo.cryptoorderbook.core.model.OrderBookPayload
import com.kwakwonjo.cryptoorderbook.core.model.OrderBookUnit
import com.kwakwonjo.cryptoorderbook.core.model.TickerSnapshot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
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
    fun `init collects orderbook and ticker into connected state`() = runTest {
        val repository = FakeOrderBookRepository(
            flowFactory = {
                flowOf(
                    OrderBookPayload(connectionState = ConnectionState.Connecting),
                    OrderBookPayload(
                        connectionState = ConnectionState.Connected,
                        orderBook = sampleOrderBook,
                    ),
                    OrderBookPayload(
                        connectionState = ConnectionState.Connected,
                        orderBook = sampleOrderBook,
                        ticker = TickerSnapshot(
                            market = sampleNavKey.market,
                            tradePrice = 101.0,
                            signedChangeRate = 0.02,
                            timestamp = 2L,
                        ),
                    ),
                )
            },
        )
        val viewModel = createViewModel(repository)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, repository.observeCalls)
        assertEquals(listOf(sampleNavKey.market), repository.observedMarkets)
        assertEquals(ConnectionState.Connected, state.connectionState)
        assertEquals(101.0, state.currentPrice)
        assertEquals(0.02, state.signedChangeRate)
        assertEquals(sampleNavKey.label, state.marketLabel)
        assertNotNull(state.orderBook)
    }

    @Test
    fun `failing flow exposes error state`() = runTest {
        val repository = FakeOrderBookRepository(
            flowFactory = {
                flow {
                    throw IllegalStateException("boom")
                }
            },
        )
        val viewModel = createViewModel(repository)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ConnectionState.Error, state.connectionState)
        assertEquals("boom", state.errorMessage)
    }

    @Test
    fun `retry restarts collection for same market`() = runTest {
        var cancellationCount = 0
        val repository = FakeOrderBookRepository(
            flowFactory = { invocation ->
                when (invocation) {
                    1 -> callbackFlow {
                        trySend(OrderBookPayload(connectionState = ConnectionState.Connecting))
                        awaitClose {
                            cancellationCount += 1
                        }
                    }

                    else -> flowOf(
                        OrderBookPayload(connectionState = ConnectionState.Connecting),
                        OrderBookPayload(
                            connectionState = ConnectionState.Connected,
                            orderBook = sampleOrderBook,
                        ),
                    )
                }
            },
        )
        val viewModel = createViewModel(repository)

        advanceUntilIdle()
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(2, repository.observeCalls)
        assertEquals(listOf(sampleNavKey.market, sampleNavKey.market), repository.observedMarkets)
        assertEquals(1, cancellationCount)
        assertEquals(ConnectionState.Connected, viewModel.uiState.value.connectionState)
        assertNotNull(viewModel.uiState.value.orderBook)
    }

    @Test
    fun `retry cancels previous job before starting another`() = runTest {
        var cancellationCount = 0
        val repository = FakeOrderBookRepository(
            flowFactory = { invocation ->
                when (invocation) {
                    1, 2 -> callbackFlow {
                        trySend(OrderBookPayload(connectionState = ConnectionState.Connecting))
                        awaitClose {
                            cancellationCount += 1
                        }
                    }

                    else -> flowOf(OrderBookPayload(connectionState = ConnectionState.Connecting))
                }
            },
        )
        val viewModel = createViewModel(repository)

        advanceUntilIdle()
        viewModel.retry()
        advanceUntilIdle()
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(3, repository.observeCalls)
        assertEquals(2, cancellationCount)
        assertTrue(repository.observedMarkets.all { it == sampleNavKey.market })
    }

    private fun createViewModel(
        repository: OrderBookRepository,
        navKey: OrderBookNavKey = sampleNavKey,
    ): OrderBookViewModel = OrderBookViewModel(
        observeOrderBookUseCase = ObserveOrderBookUseCase(repository),
        navKey = navKey,
    )

    private class FakeOrderBookRepository(
        private val flowFactory: (invocation: Int) -> Flow<OrderBookPayload>,
    ) : OrderBookRepository {
        var observeCalls: Int = 0
            private set

        val observedMarkets: MutableList<String> = mutableListOf()

        override fun observeOrderBook(market: String): Flow<OrderBookPayload> {
            observeCalls += 1
            observedMarkets += market
            return flowFactory(observeCalls)
        }
    }

    private companion object {
        val sampleNavKey = OrderBookNavKey(
            market = "KRW-BTC",
            label = "비트코인",
        )

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
