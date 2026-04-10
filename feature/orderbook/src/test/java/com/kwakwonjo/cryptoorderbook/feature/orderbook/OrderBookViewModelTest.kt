package com.kwakwonjo.cryptoorderbook.feature.orderbook

import app.cash.turbine.test
import com.kwakwonjo.cryptoorderbook.core.domain.model.OrderBookEvent
import com.kwakwonjo.cryptoorderbook.core.domain.model.OrderBookEvent.OrderBook
import com.kwakwonjo.cryptoorderbook.core.domain.model.OrderBookEvent.OrderBook.OrderBookUnit
import com.kwakwonjo.cryptoorderbook.core.domain.model.OrderBookEvent.TickerSnapshot
import com.kwakwonjo.cryptoorderbook.core.domain.repository.NetworkStatusRepository
import com.kwakwonjo.cryptoorderbook.core.domain.repository.OrderBookRepository
import com.kwakwonjo.cryptoorderbook.core.domain.usecase.IsNetworkAvailableUseCase
import com.kwakwonjo.cryptoorderbook.core.domain.usecase.ObserveConnectivityUseCase
import com.kwakwonjo.cryptoorderbook.core.domain.usecase.ObserveOrderBookUseCase
import com.kwakwonjo.cryptoorderbook.core.model.ConnectionState
import com.kwakwonjo.cryptoorderbook.core.model.MarketType
import com.kwakwonjo.cryptoorderbook.core.model.NetworkAvailability
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
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
    fun `first collect emits loading then updates accumulated content`() = runTest {
        // given
        val upstream = MutableSharedFlow<OrderBookEvent>()
        val repository = FakeOrderBookRepository(upstream)
        val networkStatusRepository = FakeNetworkStatusRepository(NetworkAvailability.CONNECTED)
        val viewModel = createViewModel(
            repository = repository,
            networkStatusRepository = networkStatusRepository,
        )

        viewModel.uiState.test {
            // when
            assertEquals(sampleLoadingState(), awaitItem())

            upstream.emit(
                OrderBookEvent(
                    connectionState = ConnectionState.Connected,
                    orderBook = sampleOrderBook,
                )
            )

            // then
            assertEquals(
                sampleLoadingState().copy(
                    orderBookData = OrderBookContract.OrderBookData(
                        orderBook = sampleOrderBook,
                        currentPrice = null,
                        signedChangeRate = null,
                    ),
                    uiStatus = OrderBookContract.UiStatus.Idle,
                ),
                awaitItem(),
            )

            upstream.emit(
                OrderBookEvent(
                    connectionState = ConnectionState.Connected,
                    orderBook = sampleOrderBook,
                    ticker = sampleTicker,
                )
            )

            assertEquals(
                sampleLoadingState().copy(
                    orderBookData = OrderBookContract.OrderBookData(
                        orderBook = sampleOrderBook,
                        currentPrice = sampleTicker.tradePrice,
                        signedChangeRate = sampleTicker.signedChangeRate,
                    ),
                    uiStatus = OrderBookContract.UiStatus.Idle,
                ),
                awaitItem(),
            )

            assertEquals(1, repository.observeCalls)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `offline initial state stays loading without starting websocket subscription`() = runTest {
        // given
        val upstream = MutableSharedFlow<OrderBookEvent>()
        val repository = FakeOrderBookRepository(upstream)
        val networkStatusRepository = FakeNetworkStatusRepository(NetworkAvailability.DISCONNECTED)
        val viewModel = createViewModel(
            repository = repository,
            networkStatusRepository = networkStatusRepository,
        )

        viewModel.uiState.test {
            // when
            val initialState = awaitItem()

            // then
            assertEquals(
                sampleLoadingState(),
                initialState,
            )
            assertEquals(0, repository.observeCalls)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `online socket failure clears content and shows socket error`() = runTest {
        // given
        val upstream = MutableSharedFlow<OrderBookEvent>()
        val repository = FakeOrderBookRepository(upstream)
        val networkStatusRepository = FakeNetworkStatusRepository(NetworkAvailability.CONNECTED)
        val viewModel = createViewModel(
            repository = repository,
            networkStatusRepository = networkStatusRepository,
        )

        viewModel.uiState.test {
            // given
            assertEquals(sampleLoadingState(), awaitItem())

            upstream.emit(
                OrderBookEvent(
                    connectionState = ConnectionState.Connected,
                    orderBook = sampleOrderBook,
                    ticker = sampleTicker,
                )
            )

            assertEquals(sampleSuccessState(), awaitItem())

            // when
            upstream.emit(
                OrderBookEvent(
                    connectionState = ConnectionState.Error,
                    errorMessage = "boom",
                )
            )

            // then
            assertEquals(
                sampleLoadingState().copy(
                    uiStatus = OrderBookContract.UiStatus.SocketError,
                ),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retry starts new collection and emits loading before next content`() = runTest {
        // given
        val firstUpstream = MutableSharedFlow<OrderBookEvent>()
        val secondUpstream = MutableSharedFlow<OrderBookEvent>()
        val repository = FakeOrderBookRepository(firstUpstream, secondUpstream)
        val networkStatusRepository = FakeNetworkStatusRepository(NetworkAvailability.CONNECTED)
        val viewModel = createViewModel(
            repository = repository,
            networkStatusRepository = networkStatusRepository,
        )

        viewModel.uiState.test {
            // given
            assertEquals(sampleLoadingState(), awaitItem())

            firstUpstream.emit(
                OrderBookEvent(
                    connectionState = ConnectionState.Connected,
                    orderBook = sampleOrderBook,
                    ticker = sampleTicker,
                )
            )

            assertEquals(sampleSuccessState(), awaitItem())

            // when
            viewModel.retry()
            advanceUntilIdle()

            // then
            assertEquals(2, repository.observeCalls)

            secondUpstream.emit(
                OrderBookEvent(
                    connectionState = ConnectionState.Connecting,
                )
            )

            assertEquals(sampleLoadingState(), awaitItem())

            secondUpstream.emit(
                OrderBookEvent(
                    connectionState = ConnectionState.Connected,
                    orderBook = updatedOrderBook,
                    ticker = updatedTicker,
                )
            )

            assertEquals(
                sampleSuccessState().copy(
                    orderBookData = OrderBookContract.OrderBookData(
                        orderBook = updatedOrderBook,
                        currentPrice = updatedTicker.tradePrice,
                        signedChangeRate = updatedTicker.signedChangeRate,
                    ),
                ),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `disconnect keeps previous content with offline state and reconnect starts a new subscription`() = runTest {
        // given
        val upstream = MutableSharedFlow<OrderBookEvent>()
        val repository = FakeOrderBookRepository(upstream)
        val networkStatusRepository = FakeNetworkStatusRepository(NetworkAvailability.CONNECTED)
        val viewModel = createViewModel(
            repository = repository,
            networkStatusRepository = networkStatusRepository,
        )

        viewModel.uiState.test {
            // given
            assertEquals(sampleLoadingState(), awaitItem())

            upstream.emit(
                OrderBookEvent(
                    connectionState = ConnectionState.Connected,
                    orderBook = sampleOrderBook,
                    ticker = sampleTicker,
                )
            )

            assertEquals(sampleSuccessState(), awaitItem())

            // when
            networkStatusRepository.setStatus(NetworkAvailability.DISCONNECTED)
            advanceUntilIdle()

            // then
            assertEquals(
                sampleSuccessState().copy(
                    uiStatus = OrderBookContract.UiStatus.Offline,
                ),
                awaitItem(),
            )

            networkStatusRepository.setStatus(NetworkAvailability.CONNECTED)
            advanceUntilIdle()

            assertEquals(2, repository.observeCalls)
            assertEquals(sampleSuccessState(), awaitItem())
            expectNoEvents()
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
        private vararg val upstreams: MutableSharedFlow<OrderBookEvent>,
    ) : OrderBookRepository {
        var observeCalls: Int = 0
            private set

        override fun observeOrderBook(market: String, orderbookUnit: Int): Flow<OrderBookEvent> {
            observeCalls += 1
            return upstreams.getOrElse(observeCalls - 1) { upstreams.last() }
        }
    }

    private class FakeNetworkStatusRepository(
        initialStatus: NetworkAvailability,
    ) : NetworkStatusRepository {
        private val connectivity = MutableStateFlow(initialStatus)

        override fun observeConnectivity(): Flow<NetworkAvailability> = connectivity

        override fun isNetworkAvailable(): Boolean = connectivity.value == NetworkAvailability.CONNECTED

        fun setStatus(status: NetworkAvailability) {
            connectivity.value = status
        }
    }

    private fun sampleLoadingState() = OrderBookContract.UiState(
        marketInfo = OrderBookContract.MarketInfo(
            market = sampleNavKey.market,
            marketType = sampleNavKey.marketType,
            koreanName = sampleNavKey.koreanName,
        ),
        orderBookData = null,
        uiStatus = OrderBookContract.UiStatus.InitialLoading,
    )

    private fun sampleSuccessState() = sampleLoadingState().copy(
        orderBookData = OrderBookContract.OrderBookData(
            orderBook = sampleOrderBook,
            currentPrice = sampleTicker.tradePrice,
            signedChangeRate = sampleTicker.signedChangeRate,
        ),
        uiStatus = OrderBookContract.UiStatus.Idle,
    )

    private companion object {
        val sampleNavKey = OrderBookNavKey(
            market = "KRW-BTC",
            marketType = MarketType.KRW,
            koreanName = "Bitcoin (KRW-BTC)",
        )

        val sampleOrderBook = OrderBook(
            market = "KRW-BTC",
            asks = listOf(OrderBookUnit(price = 102.0, size = 0.1)),
            bids = listOf(OrderBookUnit(price = 100.0, size = 0.2)),
            totalAskSize = 1.0,
            totalBidSize = 2.0,
            timestamp = 1L,
        )

        val updatedOrderBook = sampleOrderBook.copy(timestamp = 2L)

        val sampleTicker = TickerSnapshot(
            market = sampleNavKey.market,
            tradePrice = 101.0,
            signedChangeRate = 0.02,
            timestamp = 2L,
        )

        val updatedTicker = sampleTicker.copy(
            tradePrice = 103.0,
            signedChangeRate = 0.03,
            timestamp = 3L,
        )
    }
}
