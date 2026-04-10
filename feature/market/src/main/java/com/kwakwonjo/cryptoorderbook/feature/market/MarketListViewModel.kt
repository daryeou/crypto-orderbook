package com.kwakwonjo.cryptoorderbook.feature.market

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kwakwonjo.cryptoorderbook.core.domain.model.Market
import com.kwakwonjo.cryptoorderbook.core.domain.model.Ticker
import com.kwakwonjo.cryptoorderbook.core.domain.usecase.IsNetworkAvailableUseCase
import com.kwakwonjo.cryptoorderbook.core.domain.usecase.ObserveConnectivityUseCase
import com.kwakwonjo.cryptoorderbook.core.domain.usecase.GetMarketListUseCase
import com.kwakwonjo.cryptoorderbook.core.domain.usecase.GetTickerListUseCase
import com.kwakwonjo.cryptoorderbook.core.model.NetworkAvailability
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MarketListViewModel @Inject constructor(
    private val getMarketListUseCase: GetMarketListUseCase,
    private val getTickerListUseCase: GetTickerListUseCase,
    observeConnectivityUseCase: ObserveConnectivityUseCase,
    isNetworkAvailableUseCase: IsNetworkAvailableUseCase,
) : ViewModel() {

    private val refreshTrigger = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val networkAvailability: StateFlow<NetworkAvailability> = observeConnectivityUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = if (isNetworkAvailableUseCase()) {
                NetworkAvailability.CONNECTED
            } else {
                NetworkAvailability.DISCONNECTED
            },
        )

    private val marketDataFlow: Flow<MarketListEvent> = combine(
        refreshTrigger.onStart { emit(Unit) },
        networkAvailability
    ) { _, availability -> availability }
        .flatMapLatest { availability ->
            if (availability == NetworkAvailability.CONNECTED) {
                flow {
                    val markets = getMarketListUseCase()
                    emitAll(
                        getTickerListUseCase(markets.map { it.market })
                            .map { tickers ->
                                MarketListEvent(items = tickers.combineWith(markets))
                            }
                    )
                }.catch { emit(MarketListEvent(isError = true)) }
            } else {
                // 오프라인 시 관찰 중단. 이전 데이터는 아래 scan에서 보존됨
                emptyFlow()
            }
        }
        .scan(MarketListEvent()) { previous, payload ->
            // 새로운 데이터가 있으면 업데이트, 에러 시에는 이전 아이템 유지
            MarketListEvent(
                items = payload.items.ifEmpty { previous.items },
                isError = payload.isError
            )
        }

    val uiState: StateFlow<MarketListContract.UiState> = combine(
        marketDataFlow,
        networkAvailability
    ) { event, availability ->
        MarketListContract.UiState(
            items = event.items,
            uiStatus = when {
                // 네트워크는 있는데 데이터 요청에서 에러가 난 경우
                event.isError && availability == NetworkAvailability.CONNECTED ->
                    MarketListContract.UiStatus.ERROR

                // 아직 데이터가 한 번도 로드되지 않은 경우
                event.items.isEmpty() ->
                    MarketListContract.UiStatus.INITIAL_LOADING

                else -> MarketListContract.UiStatus.IDLE
            }
        )
    }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            // 시작점은 무조건 INITIAL_LOADING. 첫 데이터가 올 때까지 이 상태가 유지됨
            initialValue = MarketListContract.UiState(
                emptyList(),
                MarketListContract.UiStatus.INITIAL_LOADING
            )
        )

    fun retry() {
        refreshTrigger.tryEmit(Unit)
    }

    private fun List<Ticker>.combineWith(markets: List<Market>): List<MarketListContract.MarketItem> {
        val marketMap = markets.associateBy { it.market }
        return mapNotNull { ticker ->
            val market = marketMap[ticker.market] ?: return@mapNotNull null

            MarketListContract.MarketItem(
                market = market.market,
                marketType = market.marketType,
                koreanName = market.koreanName,
                englishName = market.englishName,
                tradePrice = ticker.tradePrice,
                signedChangeRate = ticker.signedChangeRate,
            )
        }
    }

    private data class MarketListEvent(
        val items: List<MarketListContract.MarketItem> = emptyList(),
        val isError: Boolean = false,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
