package com.kwakwonjo.cryptoorderbook.feature.orderbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kwakwonjo.cryptoorderbook.core.domain.usecase.IsNetworkAvailableUseCase
import com.kwakwonjo.cryptoorderbook.core.domain.usecase.ObserveOrderBookUseCase
import com.kwakwonjo.cryptoorderbook.core.model.ConnectionState
import com.kwakwonjo.cryptoorderbook.core.model.OrderBook
import com.kwakwonjo.cryptoorderbook.core.model.OrderBookPayload
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = OrderBookViewModel.Factory::class)
class OrderBookViewModel @AssistedInject constructor(
    private val observeOrderBookUseCase: ObserveOrderBookUseCase,
    private val isNetworkAvailableUseCase: IsNetworkAvailableUseCase,
    @Assisted private val navKey: OrderBookNavKey,
) : ViewModel() {

    private val meta = OrderBookContract.Meta(
        market = navKey.market,
        marketLabel = navKey.label,
    )

    private val refreshTrigger = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val uiState: StateFlow<OrderBookContract.UiState> = refreshTrigger
        .onStart { emit(Unit) }
        .flatMapLatest {
            if (!isNetworkAvailableUseCase()) {
                flowOf(
                    OrderBookContract.UiState.Error(
                        meta = meta,
                        type = OrderBookContract.ErrorType.NETWORK,
                    )
                )
            } else {
                observeOrderBookUseCase(navKey.market)
                    .conflate()
                    .scan(OrderBookAccumulator(meta = meta)) { accumulator, payload ->
                        accumulator.reduce(
                            payload = payload,
                            isNetworkAvailable = isNetworkAvailableUseCase(),
                        )
                    }
                    .map(OrderBookAccumulator::toUiState)
                    .onStart {
                        emit(OrderBookContract.UiState.Loading(meta))
                    }
                    .catch {
                        emit(
                            OrderBookContract.UiState.Error(
                                meta = meta,
                                type = resolveErrorType(),
                            )
                        )
                    }
            }
        }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = OrderBookContract.UiState.Loading(meta),
        )

    fun retry() {
        refreshTrigger.tryEmit(Unit)
    }

    @AssistedFactory
    interface Factory {
        fun create(navKey: OrderBookNavKey): OrderBookViewModel
    }

    private fun resolveErrorType(): OrderBookContract.ErrorType {
        return if (isNetworkAvailableUseCase()) {
            OrderBookContract.ErrorType.SOCKET
        } else {
            OrderBookContract.ErrorType.NETWORK
        }
    }

    private data class OrderBookAccumulator(
        val meta: OrderBookContract.Meta,
        val orderBook: OrderBook? = null,
        val currentPrice: Double? = null,
        val signedChangeRate: Double? = null,
        val errorType: OrderBookContract.ErrorType? = null,
    ) {
        fun reduce(
            payload: OrderBookPayload,
            isNetworkAvailable: Boolean,
        ): OrderBookAccumulator = when (payload.connectionState) {
            ConnectionState.Error -> copy(
                errorType = if (isNetworkAvailable) {
                    OrderBookContract.ErrorType.SOCKET
                } else {
                    OrderBookContract.ErrorType.NETWORK
                },
            )

            else -> copy(
                orderBook = payload.orderBook ?: orderBook,
                currentPrice = payload.ticker?.tradePrice ?: currentPrice,
                signedChangeRate = payload.ticker?.signedChangeRate ?: signedChangeRate,
                errorType = null,
            )
        }

        fun toUiState(): OrderBookContract.UiState {
            return when {
                errorType != null -> OrderBookContract.UiState.Error(
                    meta = meta,
                    type = errorType,
                )

                orderBook != null -> OrderBookContract.UiState.Success(
                    meta = meta,
                    orderBook = orderBook,
                    currentPrice = currentPrice,
                    signedChangeRate = signedChangeRate,
                )

                else -> OrderBookContract.UiState.Loading(meta)
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
