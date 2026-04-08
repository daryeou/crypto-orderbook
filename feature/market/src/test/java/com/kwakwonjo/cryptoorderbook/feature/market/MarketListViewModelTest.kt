package com.kwakwonjo.cryptoorderbook.feature.market

import app.cash.turbine.test
import com.kwakwonjo.cryptoorderbook.core.domain.repository.MarketRepository
import com.kwakwonjo.cryptoorderbook.core.domain.usecase.ObserveMarketSummariesUseCase
import com.kwakwonjo.cryptoorderbook.core.model.MarketSummary
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
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
        val upstream = MutableSharedFlow<List<MarketSummary>>()
        val repository = FakeMarketRepository(
            flowFactory = {
                upstream
            },
        )
        val viewModel = createViewModel(repository)

        assertEquals(0, repository.observeCalls)

        viewModel.uiState.test {
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

            assertEquals(
                MarketListContract.UiState.Success(markets),
                awaitItem(),
            )
            assertEquals(1, repository.observeCalls)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `load failure exposes error state`() = runTest {
        val failGate = CompletableDeferred<Unit>()
        val viewModel = createViewModel(
            FakeMarketRepository(
                flowFactory = {
                    flow {
                        failGate.await()
                        throw IllegalStateException("boom")
                    }
                },
            )
        )

        viewModel.uiState.test {
            assertEquals(MarketListContract.UiState.Loading, awaitItem())

            failGate.complete(Unit)

            assertEquals(
                MarketListContract.UiState.Error,
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retry restarts polling flow`() = runTest {
        var cancellationCount = 0
        val firstUpstream = MutableSharedFlow<List<MarketSummary>>()
        val secondUpstream = MutableSharedFlow<List<MarketSummary>>()
        val firstMarkets = listOf(
            MarketSummary(
                market = "KRW-BTC",
                koreanName = "비트코인",
                englishName = "Bitcoin",
                tradePrice = 100.0,
                signedChangeRate = 0.01,
            )
        )
        val secondMarkets = listOf(
            MarketSummary(
                market = "KRW-ETH",
                koreanName = "이더리움",
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
        val viewModel = createViewModel(repository)

        viewModel.uiState.test {
            assertEquals(MarketListContract.UiState.Loading, awaitItem())

            firstUpstream.emit(firstMarkets)
            assertEquals(MarketListContract.UiState.Success(firstMarkets), awaitItem())

            viewModel.retry()

            assertEquals(MarketListContract.UiState.Loading, awaitItem())

            secondUpstream.emit(secondMarkets)
            assertEquals(MarketListContract.UiState.Success(secondMarkets), awaitItem())
            assertEquals(2, repository.observeCalls)
            assertEquals(1, cancellationCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createViewModel(
        repository: MarketRepository,
    ): MarketListViewModel = MarketListViewModel(
        observeMarketSummariesUseCase = ObserveMarketSummariesUseCase(repository),
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
}
