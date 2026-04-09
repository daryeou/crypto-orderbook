package com.kwakwonjo.cryptoorderbook.feature.market

import app.cash.turbine.test
import com.kwakwonjo.cryptoorderbook.core.domain.repository.MarketRepository
import com.kwakwonjo.cryptoorderbook.core.domain.repository.NetworkStatusRepository
import com.kwakwonjo.cryptoorderbook.core.domain.usecase.IsNetworkAvailableUseCase
import com.kwakwonjo.cryptoorderbook.core.domain.usecase.ObserveConnectivityUseCase
import com.kwakwonjo.cryptoorderbook.core.domain.usecase.ObserveMarketSummariesUseCase
import com.kwakwonjo.cryptoorderbook.core.model.ConnectivityStatus
import com.kwakwonjo.cryptoorderbook.core.model.MarketSummary
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
class MarketListViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun `first collect emits loading then success`() = runTest {
        // given
        val upstream = MutableSharedFlow<List<MarketSummary>>()
        val repository = FakeMarketRepository(
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
            assertEquals(MarketListContract.UiState.Loading, awaitItem())

            val markets = listOf(
                MarketSummary(
                    market = "KRW-BTC",
                    koreanName = "비트코인",
                    englishName = "Bitcoin",
                    tradePrice = 100.0,
                    signedChangeRate = 0.01,
                )
            )
            upstream.emit(markets)

            // then
            assertEquals(
                MarketListContract.UiState.Success(markets),
                awaitItem(),
            )
            assertEquals(1, repository.observeCalls)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `offline state keeps loading without starting polling`() = runTest {
        // given
        val repository = FakeMarketRepository(
            flowFactory = {
                error("Polling should not start while offline.")
            },
        )
        val networkStatusRepository = FakeNetworkStatusRepository(ConnectivityStatus.DISCONNECTED)
        val viewModel = createViewModel(
            repository = repository,
            networkStatusRepository = networkStatusRepository,
        )

        viewModel.uiState.test {
            // when
            assertEquals(MarketListContract.UiState.Loading, awaitItem())

            // then
            expectNoEvents()
            assertEquals(0, repository.observeCalls)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `online polling failure exposes error state`() = runTest {
        // given
        val failGate = CompletableDeferred<Unit>()
        val networkStatusRepository = FakeNetworkStatusRepository(ConnectivityStatus.CONNECTED)
        val viewModel = createViewModel(
            repository = FakeMarketRepository(
                flowFactory = {
                    flow {
                        failGate.await()
                        throw IllegalStateException("boom")
                    }
                },
            ),
            networkStatusRepository = networkStatusRepository,
        )

        viewModel.uiState.test {
            // when
            assertEquals(MarketListContract.UiState.Loading, awaitItem())

            failGate.complete(Unit)

            // then
            assertEquals(
                MarketListContract.UiState.Error,
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retry after error restarts polling and emits loading again`() = runTest {
        // given
        val secondUpstream = MutableSharedFlow<List<MarketSummary>>()
        val failGate = CompletableDeferred<Unit>()
        val repository = FakeMarketRepository(
            flowFactory = { invocation ->
                when (invocation) {
                    1 -> flow {
                        failGate.await()
                        throw IllegalStateException("boom")
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
        val markets = listOf(
            MarketSummary(
                market = "KRW-BTC",
                koreanName = "Bitcoin",
                englishName = "Bitcoin",
                tradePrice = 100.0,
                signedChangeRate = 0.01,
            )
        )

        viewModel.uiState.test {
            // when
            assertEquals(MarketListContract.UiState.Loading, awaitItem())

            failGate.complete(Unit)
            assertEquals(MarketListContract.UiState.Error, awaitItem())

            viewModel.retry()
            advanceUntilIdle()
            assertEquals(MarketListContract.UiState.Loading, awaitItem())

            secondUpstream.emit(markets)

            // then
            assertEquals(MarketListContract.UiState.Success(markets), awaitItem())
            assertEquals(2, repository.observeCalls)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `disconnect keeps last success and reconnect automatically restarts polling`() = runTest {
        // given
        var cancellationCount = 0
        val firstUpstream = MutableSharedFlow<List<MarketSummary>>()
        val secondUpstream = MutableSharedFlow<List<MarketSummary>>()
        val firstMarkets = listOf(
            MarketSummary(
                market = "KRW-BTC",
                koreanName = "Bitcoin",
                englishName = "Bitcoin",
                tradePrice = 100.0,
                signedChangeRate = 0.01,
            )
        )
        val secondMarkets = listOf(
            MarketSummary(
                market = "KRW-ETH",
                koreanName = "Ethereum",
                englishName = "Ethereum",
                tradePrice = 200.0,
                signedChangeRate = -0.02,
            )
        )
        val repository = FakeMarketRepository(
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
            assertEquals(MarketListContract.UiState.Loading, awaitItem())

            firstUpstream.emit(firstMarkets)
            assertEquals(MarketListContract.UiState.Success(firstMarkets), awaitItem())

            networkStatusRepository.setStatus(ConnectivityStatus.DISCONNECTED)
            advanceUntilIdle()

            assertEquals(1, cancellationCount)
            expectNoEvents()

            networkStatusRepository.setStatus(ConnectivityStatus.CONNECTED)
            advanceUntilIdle()

            assertEquals(2, repository.observeCalls)

            secondUpstream.emit(secondMarkets)

            // then
            assertEquals(MarketListContract.UiState.Success(secondMarkets), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createViewModel(
        repository: MarketRepository,
        networkStatusRepository: FakeNetworkStatusRepository,
    ): MarketListViewModel = MarketListViewModel(
        observeMarketSummariesUseCase = ObserveMarketSummariesUseCase(repository),
        observeConnectivityUseCase = ObserveConnectivityUseCase(networkStatusRepository),
        isNetworkAvailableUseCase = IsNetworkAvailableUseCase(networkStatusRepository),
    )

    private class FakeMarketRepository(
        private val flowFactory: (invocation: Int) -> Flow<List<MarketSummary>>,
    ) : MarketRepository {
        var observeCalls: Int = 0
            private set

        override fun observeMarketSummaries(pollingIntervalMillis: Long): Flow<List<MarketSummary>> {
            observeCalls += 1
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
}
