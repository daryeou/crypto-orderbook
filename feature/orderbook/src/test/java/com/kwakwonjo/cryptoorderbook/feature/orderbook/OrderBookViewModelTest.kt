package com.kwakwonjo.cryptoorderbook.feature.orderbook

import app.cash.turbine.test
import com.kwakwonjo.cryptoorderbook.core.domain.repository.NetworkStatusRepository
import com.kwakwonjo.cryptoorderbook.core.domain.repository.OrderBookRepository
import com.kwakwonjo.cryptoorderbook.core.domain.usecase.IsNetworkAvailableUseCase
import com.kwakwonjo.cryptoorderbook.core.domain.usecase.ObserveConnectivityUseCase
import com.kwakwonjo.cryptoorderbook.core.domain.usecase.ObserveOrderBookUseCase
import com.kwakwonjo.cryptoorderbook.core.model.ConnectivityStatus
import com.kwakwonjo.cryptoorderbook.core.model.ConnectionState
import com.kwakwonjo.cryptoorderbook.core.model.OrderBook
import com.kwakwonjo.cryptoorderbook.core.model.OrderBookPayload
import com.kwakwonjo.cryptoorderbook.core.model.OrderBookUnit
import com.kwakwonjo.cryptoorderbook.core.model.TickerSnapshot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrderBookViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun `first collect emits loading then accumulates orderbook and ticker into success`() = runTest {
        // given
        val upstream = MutableSharedFlow<OrderBookPayload>()
        val repository = FakeOrderBookRepository(
            flowFactory = {
                upstream
            },
        )
        val networkStatusRepository = FakeNetworkStatusRepository(ConnectivityStatus.CONNECTED)
        val viewModel = createViewModel(
            repository = repository,
            networkStatusRepository = networkStatusRepository,
        )

        assertEquals(0, repository.observeCalls)

        viewModel.uiState.test {
            // when
            assertEquals(OrderBookContract.UiState.Loading(sampleMeta), awaitItem())

            upstream.emit(
                OrderBookPayload(
                    connectionState = ConnectionState.Connected,
                    orderBook = sampleOrderBook,
                )
            )
            assertEquals(
                OrderBookContract.UiState.Success(
                    meta = sampleMeta,
                    orderBook = sampleOrderBook,
                    currentPrice = null,
                    signedChangeRate = null,
                ),
                awaitItem(),
            )

            upstream.emit(
                OrderBookPayload(
                    connectionState = ConnectionState.Connected,
                    ticker = TickerSnapshot(
                        market = sampleNavKey.market,
                        tradePrice = 101.0,
                        signedChangeRate = 0.02,
                        timestamp = 2L,
                    ),
                )
            )
            assertEquals(
                OrderBookContract.UiState.Success(
                    meta = sampleMeta,
                    orderBook = sampleOrderBook,
                    currentPrice = 101.0,
                    signedChangeRate = 0.02,
                ),
                awaitItem(),
            )

            // then
            assertEquals(1, repository.observeCalls)
            assertEquals(listOf(sampleNavKey.market), repository.observedMarkets)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `offline state keeps loading without websocket subscription`() = runTest {
        // given
        val repository = FakeOrderBookRepository(
            flowFactory = {
                error("WebSocket collection should not start while offline.")
            },
        )
        val networkStatusRepository = FakeNetworkStatusRepository(ConnectivityStatus.DISCONNECTED)
        val viewModel = createViewModel(
            repository = repository,
            networkStatusRepository = networkStatusRepository,
        )

        viewModel.uiState.test {
            // when
            assertEquals(OrderBookContract.UiState.Loading(sampleMeta), awaitItem())

            // then
            expectNoEvents()
            assertEquals(0, repository.observeCalls)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `failing flow while online maps to socket error state`() = runTest {
        // given
        val failGate = CompletableDeferred<Unit>()
        val repository = FakeOrderBookRepository(
            flowFactory = {
                flow {
                    failGate.await()
                    throw IllegalStateException("boom")
                }
            },
        )
        val networkStatusRepository = FakeNetworkStatusRepository(ConnectivityStatus.CONNECTED)
        val viewModel = createViewModel(
            repository = repository,
            networkStatusRepository = networkStatusRepository,
        )

        viewModel.uiState.test {
            // when
            assertEquals(OrderBookContract.UiState.Loading(sampleMeta), awaitItem())

            failGate.complete(Unit)

            // then
            assertEquals(
                OrderBookContract.UiState.Error(
                    meta = sampleMeta,
                    type = OrderBookContract.ErrorType.SOCKET,
                ),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retry starts new websocket collection without forcing loading after success`() = runTest {
        // given
        var cancellationCount = 0
        val firstUpstream = MutableSharedFlow<OrderBookPayload>()
        val secondUpstream = MutableSharedFlow<OrderBookPayload>()
        val updatedOrderBook = sampleOrderBook.copy(timestamp = 2L)
        val repository = FakeOrderBookRepository(
            flowFactory = { invocation ->
                when (invocation) {
                    1 -> flow {
                        try {
                            emitAll(firstUpstream)
                            awaitCancellation()
                        } finally {
                            cancellationCount += 1
                        }
                    }

                    else -> secondUpstream
                }
            },
        )
        val networkStatusRepository = FakeNetworkStatusRepository(ConnectivityStatus.CONNECTED)
        val viewModel = createViewModel(
            repository = repository,
            networkStatusRepository = networkStatusRepository,
        )

        viewModel.uiState.test {
            // when
            assertEquals(OrderBookContract.UiState.Loading(sampleMeta), awaitItem())

            firstUpstream.emit(
                OrderBookPayload(
                    connectionState = ConnectionState.Connected,
                    orderBook = sampleOrderBook,
                )
            )
            assertEquals(
                OrderBookContract.UiState.Success(
                    meta = sampleMeta,
                    orderBook = sampleOrderBook,
                    currentPrice = null,
                    signedChangeRate = null,
                ),
                awaitItem(),
            )

            viewModel.retry()
            advanceUntilIdle()

            assertEquals(2, repository.observeCalls)
            assertEquals(1, cancellationCount)

            secondUpstream.emit(
                OrderBookPayload(
                    connectionState = ConnectionState.Connected,
                    orderBook = updatedOrderBook,
                )
            )

            // then
            assertEquals(
                OrderBookContract.UiState.Success(
                    meta = sampleMeta,
                    orderBook = updatedOrderBook,
                    currentPrice = null,
                    signedChangeRate = null,
                ),
                awaitItem(),
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `disconnect keeps last success and reconnect automatically starts new subscription`() = runTest {
        // given
        var cancellationCount = 0
        val firstUpstream = MutableSharedFlow<OrderBookPayload>()
        val secondUpstream = MutableSharedFlow<OrderBookPayload>()
        val updatedOrderBook = sampleOrderBook.copy(timestamp = 3L)
        val repository = FakeOrderBookRepository(
            flowFactory = { invocation ->
                when (invocation) {
                    1 -> flow {
                        try {
                            emitAll(firstUpstream)
                            awaitCancellation()
                        } finally {
                            cancellationCount += 1
                        }
                    }

                    else -> secondUpstream
                }
            },
        )
        val networkStatusRepository = FakeNetworkStatusRepository(ConnectivityStatus.CONNECTED)
        val viewModel = createViewModel(
            repository = repository,
            networkStatusRepository = networkStatusRepository,
        )

        viewModel.uiState.test {
            // when
            assertEquals(OrderBookContract.UiState.Loading(sampleMeta), awaitItem())

            firstUpstream.emit(
                OrderBookPayload(
                    connectionState = ConnectionState.Connected,
                    orderBook = sampleOrderBook,
                )
            )
            assertEquals(
                OrderBookContract.UiState.Success(
                    meta = sampleMeta,
                    orderBook = sampleOrderBook,
                    currentPrice = null,
                    signedChangeRate = null,
                ),
                awaitItem(),
            )

            networkStatusRepository.setStatus(ConnectivityStatus.DISCONNECTED)
            advanceUntilIdle()

            assertEquals(1, cancellationCount)
            expectNoEvents()

            networkStatusRepository.setStatus(ConnectivityStatus.CONNECTED)
            advanceUntilIdle()

            assertEquals(2, repository.observeCalls)

            secondUpstream.emit(
                OrderBookPayload(
                    connectionState = ConnectionState.Connected,
                    orderBook = updatedOrderBook,
                )
            )

            // then
            assertEquals(
                OrderBookContract.UiState.Success(
                    meta = sampleMeta,
                    orderBook = updatedOrderBook,
                    currentPrice = null,
                    signedChangeRate = null,
                ),
                awaitItem(),
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createViewModel(
        repository: OrderBookRepository,
        networkStatusRepository: FakeNetworkStatusRepository,
        navKey: OrderBookNavKey = sampleNavKey,
    ): OrderBookViewModel = OrderBookViewModel(
        observeOrderBookUseCase = ObserveOrderBookUseCase(repository),
        isNetworkAvailableUseCase = IsNetworkAvailableUseCase(
            networkStatusRepository = networkStatusRepository,
        ),
        observeConnectivityUseCase = ObserveConnectivityUseCase(
            networkStatusRepository = networkStatusRepository,
        ),
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

    private class FakeNetworkStatusRepository(
        initialStatus: ConnectivityStatus,
    ) : NetworkStatusRepository {
        private val connectivity = MutableStateFlow(initialStatus)

        override fun observeConnectivity(): Flow<ConnectivityStatus> = connectivity

        override fun isNetworkAvailable(): Boolean = connectivity.value == ConnectivityStatus.CONNECTED

        fun setStatus(status: ConnectivityStatus) {
            connectivity.value = status
        }
    }

    private companion object {
        val sampleNavKey = OrderBookNavKey(
            market = "KRW-BTC",
            label = "Bitcoin (KRW-BTC)",
        )

        val sampleMeta = OrderBookContract.Meta(
            market = sampleNavKey.market,
            marketLabel = sampleNavKey.label,
        )

        val sampleOrderBook = OrderBook(
            market = "KRW-BTC",
            asks = listOf(OrderBookUnit(price = 102.0, size = 0.1)),
            bids = listOf(OrderBookUnit(price = 100.0, size = 0.2)),
            totalAskSize = 1.0,
            totalBidSize = 2.0,
            timestamp = 1L,
        )
    }
}
