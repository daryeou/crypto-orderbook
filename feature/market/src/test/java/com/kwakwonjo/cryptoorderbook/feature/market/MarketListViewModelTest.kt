package com.kwakwonjo.cryptoorderbook.feature.market

import app.cash.turbine.test
import com.kwakwonjo.cryptoorderbook.core.model.Market
import com.kwakwonjo.cryptoorderbook.core.model.Ticker
import com.kwakwonjo.cryptoorderbook.core.domain.repository.MarketRepository
import com.kwakwonjo.cryptoorderbook.core.domain.repository.NetworkStatusRepository
import com.kwakwonjo.cryptoorderbook.core.domain.usecase.GetMarketListUseCase
import com.kwakwonjo.cryptoorderbook.core.domain.usecase.GetTickerListUseCase
import com.kwakwonjo.cryptoorderbook.core.domain.usecase.IsNetworkAvailableUseCase
import com.kwakwonjo.cryptoorderbook.core.domain.usecase.ObserveConnectivityUseCase
import com.kwakwonjo.cryptoorderbook.core.model.NetworkAvailability
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MarketListViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun `offline initial state exposes error without starting market requests`() = runTest {
        val repository = FakeMarketRepository()
        val viewModel = createViewModel(
            repository = repository,
            networkStatusRepository = FakeNetworkStatusRepository(NetworkAvailability.DISCONNECTED),
        )

        viewModel.uiState.test {
            assertEquals(
                MarketListContract.UiState(
                    items = emptyList(),
                    uiStatus = MarketListContract.UiStatus.ERROR,
                ),
                awaitItem(),
            )

            assertEquals(0, repository.fetchMarketListCalls)
            assertEquals(emptyList<List<String>>(), repository.fetchTickerListRequests)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `online ticker failure exposes error state`() = runTest {
        val repository = FakeMarketRepository().apply {
            enqueueMarketListSuccess(listOf(market("KRW-BTC")))
            enqueueTickerListFailure(IllegalStateException("boom"))
        }
        val viewModel = createViewModel(
            repository = repository,
            networkStatusRepository = FakeNetworkStatusRepository(NetworkAvailability.CONNECTED),
        )

        viewModel.uiState.test {
            assertEquals(
                MarketListContract.UiState(
                    items = emptyList(),
                    uiStatus = MarketListContract.UiStatus.ERROR,
                ),
                awaitItem(),
            )

            assertEquals(1, repository.fetchMarketListCalls)
            assertEquals(listOf(listOf("KRW-BTC")), repository.fetchTickerListRequests)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retry after error restarts market loading and emits success`() = runTest {
        val markets = listOf(market("KRW-BTC", "Bitcoin KRW", "Bitcoin"))
        val tickers = listOf(ticker("KRW-BTC", tradePrice = 148_956_000.0, signedChangeRate = 0.0031))
        val repository = FakeMarketRepository().apply {
            enqueueMarketListSuccess(markets)
            enqueueTickerListFailure(IllegalStateException("boom"))
            enqueueMarketListSuccess(markets)
            enqueueTickerListSuccess(tickers)
        }
        val viewModel = createViewModel(
            repository = repository,
            networkStatusRepository = FakeNetworkStatusRepository(NetworkAvailability.CONNECTED),
        )

        viewModel.uiState.test {
            assertEquals(
                MarketListContract.UiState(
                    items = emptyList(),
                    uiStatus = MarketListContract.UiStatus.ERROR,
                ),
                awaitItem(),
            )

            viewModel.retry()
            runCurrent()

            assertEquals(
                MarketListContract.UiState(
                    items = listOf(marketItem(markets[0], tickers[0])),
                    uiStatus = MarketListContract.UiStatus.IDLE,
                ),
                awaitItem(),
            )
            assertEquals(2, repository.fetchMarketListCalls)
            assertEquals(
                listOf(
                    listOf("KRW-BTC"),
                    listOf("KRW-BTC"),
                ),
                repository.fetchTickerListRequests,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `reconnect automatically reloads market data after disconnect`() = runTest {
        val firstMarkets = listOf(market("KRW-BTC", "Bitcoin KRW", "Bitcoin"))
        val firstTickers = listOf(ticker("KRW-BTC", tradePrice = 148_956_000.0, signedChangeRate = 0.0031))
        val secondMarkets = listOf(market("KRW-ETH", "Ethereum KRW", "Ethereum"))
        val secondTickers = listOf(ticker("KRW-ETH", tradePrice = 5_486_000.0, signedChangeRate = -0.0124))
        val repository = FakeMarketRepository().apply {
            enqueueMarketListSuccess(firstMarkets)
            enqueueTickerListSuccess(firstTickers)
            enqueueMarketListSuccess(secondMarkets)
            enqueueTickerListSuccess(secondTickers)
        }
        val networkStatusRepository = FakeNetworkStatusRepository(NetworkAvailability.CONNECTED)
        val viewModel = createViewModel(
            repository = repository,
            networkStatusRepository = networkStatusRepository,
        )

        viewModel.uiState.test {
            assertEquals(
                MarketListContract.UiState(
                    items = listOf(marketItem(firstMarkets[0], firstTickers[0])),
                    uiStatus = MarketListContract.UiStatus.IDLE,
                ),
                awaitItem(),
            )

            networkStatusRepository.setStatus(NetworkAvailability.DISCONNECTED)
            runCurrent()
            assertEquals(
                MarketListContract.UiState(
                    items = listOf(marketItem(firstMarkets[0], firstTickers[0])),
                    uiStatus = MarketListContract.UiStatus.ERROR,
                ),
                awaitItem(),
            )

            networkStatusRepository.setStatus(NetworkAvailability.CONNECTED)
            runCurrent()

            assertEquals(
                MarketListContract.UiState(
                    items = listOf(marketItem(firstMarkets[0], firstTickers[0])),
                    uiStatus = MarketListContract.UiStatus.IDLE,
                ),
                awaitItem(),
            )

            assertEquals(
                MarketListContract.UiState(
                    items = listOf(marketItem(secondMarkets[0], secondTickers[0])),
                    uiStatus = MarketListContract.UiStatus.IDLE,
                ),
                awaitItem(),
            )
            assertEquals(2, repository.fetchMarketListCalls)
            assertEquals(
                listOf(
                    listOf("KRW-BTC"),
                    listOf("KRW-ETH"),
                ),
                repository.fetchTickerListRequests,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `success state keeps all supported market types`() = runTest {
        val markets = listOf(
            market("KRW-BTC", "Bitcoin KRW", "Bitcoin"),
            market("BTC-ETH", "Ethereum BTC", "Ethereum"),
            market("USDT-XRP", "XRP USDT", "XRP"),
        )
        val tickers = listOf(
            ticker("KRW-BTC", tradePrice = 148_956_000.0, signedChangeRate = 0.0031),
            ticker("BTC-ETH", tradePrice = 0.0345, signedChangeRate = -0.0124),
            ticker("USDT-XRP", tradePrice = 1.95, signedChangeRate = 0.021),
        )
        val repository = FakeMarketRepository().apply {
            enqueueMarketListSuccess(markets)
            enqueueTickerListSuccess(tickers)
        }
        val viewModel = createViewModel(
            repository = repository,
            networkStatusRepository = FakeNetworkStatusRepository(NetworkAvailability.CONNECTED),
        )

        viewModel.uiState.test {
            assertEquals(
                MarketListContract.UiState(
                    items = listOf(
                        marketItem(markets[0], tickers[0]),
                        marketItem(markets[1], tickers[1]),
                        marketItem(markets[2], tickers[2]),
                    ),
                    uiStatus = MarketListContract.UiStatus.IDLE,
                ),
                awaitItem(),
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `success state ignores tickers that are not in fetched markets`() = runTest {
        val markets = listOf(
            market("KRW-BTC", "Bitcoin KRW", "Bitcoin"),
            market("KRW-ETH", "Ethereum KRW", "Ethereum"),
        )
        val tickers = listOf(
            ticker("KRW-XRP", tradePrice = 3_000.0, signedChangeRate = 0.05),
            ticker("KRW-BTC", tradePrice = 148_956_000.0, signedChangeRate = 0.0031),
            ticker("KRW-ETH", tradePrice = 5_486_000.0, signedChangeRate = -0.0124),
        )
        val repository = FakeMarketRepository().apply {
            enqueueMarketListSuccess(markets)
            enqueueTickerListSuccess(tickers)
        }
        val viewModel = createViewModel(
            repository = repository,
            networkStatusRepository = FakeNetworkStatusRepository(NetworkAvailability.CONNECTED),
        )

        viewModel.uiState.test {
            assertEquals(
                MarketListContract.UiState(
                    items = listOf(
                        marketItem(markets[0], tickers[1]),
                        marketItem(markets[1], tickers[2]),
                    ),
                    uiStatus = MarketListContract.UiStatus.IDLE,
                ),
                awaitItem(),
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createViewModel(
        repository: MarketRepository,
        networkStatusRepository: FakeNetworkStatusRepository,
    ): MarketListViewModel = MarketListViewModel(
        getMarketListUseCase = GetMarketListUseCase(repository),
        getTickerListUseCase = GetTickerListUseCase(repository),
        observeConnectivityUseCase = ObserveConnectivityUseCase(networkStatusRepository),
        isNetworkAvailableUseCase = IsNetworkAvailableUseCase(networkStatusRepository),
    )

    private fun market(
        market: String,
        koreanName: String = market,
        englishName: String = market,
    ): Market = Market(
        market = market,
        koreanName = koreanName,
        englishName = englishName,
    )

    private fun ticker(
        market: String,
        tradePrice: Double,
        signedChangeRate: Double,
    ): Ticker = Ticker(
        market = market,
        tradePrice = tradePrice,
        signedChangeRate = signedChangeRate,
    )

    private fun marketItem(
        market: Market,
        ticker: Ticker,
    ): MarketListContract.MarketItem = MarketListContract.MarketItem(
        market = market,
        ticker = ticker,
    )

    private sealed interface MarketListResult {
        data class Success(
            val markets: List<Market>,
        ) : MarketListResult

        data class Failure(
            val throwable: Throwable,
        ) : MarketListResult
    }

    private sealed interface TickerListResult {
        data class Success(
            val tickers: List<Ticker>,
        ) : TickerListResult

        data class Failure(
            val throwable: Throwable,
        ) : TickerListResult
    }

    private class FakeMarketRepository : MarketRepository {
        private val marketListResults = ArrayDeque<MarketListResult>()
        private val tickerListResults = ArrayDeque<TickerListResult>()

        var fetchMarketListCalls: Int = 0
            private set

        val fetchTickerListRequests = mutableListOf<List<String>>()

        override suspend fun fetchMarketList(): List<Market> {
            fetchMarketListCalls += 1
            return when (val result = marketListResults.removeFirst()) {
                is MarketListResult.Success -> result.markets
                is MarketListResult.Failure -> throw result.throwable
            }
        }

        override suspend fun fetchTickerList(markets: List<String>): List<Ticker> {
            fetchTickerListRequests += markets
            return when (val result = tickerListResults.removeFirst()) {
                is TickerListResult.Success -> result.tickers
                is TickerListResult.Failure -> throw result.throwable
            }
        }

        fun enqueueMarketListSuccess(markets: List<Market>) {
            marketListResults.addLast(MarketListResult.Success(markets))
        }

        fun enqueueTickerListSuccess(tickers: List<Ticker>) {
            tickerListResults.addLast(TickerListResult.Success(tickers))
        }

        fun enqueueTickerListFailure(throwable: Throwable) {
            tickerListResults.addLast(TickerListResult.Failure(throwable))
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
}
