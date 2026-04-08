package com.kmwh.cryptoorderbook.feature.market

import com.kmwh.cryptoorderbook.core.domain.repository.MarketRepository
import com.kmwh.cryptoorderbook.core.domain.usecase.ObserveMarketSummariesUseCase
import com.kmwh.cryptoorderbook.core.model.MarketSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MarketListViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun `init loads market list into success state`() = runTest {
        val viewModel = MarketListViewModel(
            observeMarketSummariesUseCase = ObserveMarketSummariesUseCase(
                marketRepository = FakeMarketRepository(
                    result = Result.success(
                        listOf(
                            MarketSummary(
                                market = "KRW-BTC",
                                koreanName = "비트코인",
                                englishName = "Bitcoin",
                                tradePrice = 100.0,
                                signedChangeRate = 0.01,
                            )
                        )
                    )
                )
            )
        )

        viewModel.startPolling()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is MarketListUiState.Success)
        assertEquals(1, (state as MarketListUiState.Success).markets.size)
        assertEquals("KRW-BTC", state.markets.first().market)
    }

    @Test
    fun `load failure exposes error state`() = runTest {
        val viewModel = MarketListViewModel(
            observeMarketSummariesUseCase = ObserveMarketSummariesUseCase(
                marketRepository = FakeMarketRepository(
                    result = Result.failure(IllegalStateException("boom"))
                )
            )
        )

        viewModel.startPolling()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is MarketListUiState.Error)
        assertEquals("boom", (state as MarketListUiState.Error).message)
    }

    private class FakeMarketRepository(
        private val result: Result<List<MarketSummary>>,
    ) : MarketRepository {
        override fun observeMarketSummaries(pollingIntervalMillis: Long) = flow {
            emit(result.getOrThrow())
        }
    }
}
