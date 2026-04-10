package com.kwakwonjo.cryptoorderbook.feature.market

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kwakwonjo.cryptoorderbook.core.model.Market
import com.kwakwonjo.cryptoorderbook.core.model.Ticker
import com.kwakwonjo.cryptoorderbook.core.domain.usecase.GetMarketListUseCase
import com.kwakwonjo.cryptoorderbook.core.domain.usecase.GetTickerListUseCase
import com.kwakwonjo.cryptoorderbook.core.domain.usecase.IsNetworkAvailableUseCase
import com.kwakwonjo.cryptoorderbook.core.domain.usecase.ObserveConnectivityUseCase
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
        networkAvailability,
    ) { _, availability ->
        availability
    }
        .flatMapLatest { availability ->
            if (availability != NetworkAvailability.CONNECTED) {
                return@flatMapLatest emptyFlow()
            }

            flow {
                val markets = getMarketListUseCase()
                emitAll(
                    getTickerListUseCase(markets.map { it.market })
                        .map { tickers ->
                            MarketListEvent(items = tickers.combineWith(markets))
                        },
                )
            }.catch {
                emit(MarketListEvent(isError = true))
            }
        }
        .scan(MarketListEvent()) { previous, payload ->
            MarketListEvent(
                items = payload.items.ifEmpty { previous.items },
                isError = payload.isError,
            )
        }

    val uiState: StateFlow<MarketListContract.UiState> = combine(
        marketDataFlow,
        networkAvailability,
    ) { event, availability ->
        MarketListContract.UiState(
            items = event.items,
            uiStatus = when {
                event.isError || availability == NetworkAvailability.DISCONNECTED -> {
                    MarketListContract.UiStatus.ERROR
                }

                event.items.isEmpty() -> {
                    MarketListContract.UiStatus.INITIAL_LOADING
                }

                else -> MarketListContract.UiStatus.IDLE
            },
        )
    }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = MarketListContract.UiState(
                items = emptyList(),
                uiStatus = MarketListContract.UiStatus.INITIAL_LOADING,
            ),
        )

    fun retry() {
        refreshTrigger.tryEmit(Unit)
    }

    private fun List<Ticker>.combineWith(markets: List<Market>): List<MarketListContract.MarketItem> {
        val marketMap = markets.associateBy { it.market }

        return mapNotNull { ticker ->
            val market = marketMap[ticker.market] ?: return@mapNotNull null

            MarketListContract.MarketItem(
                market = market,
                ticker = ticker,
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
