package com.kwakwonjo.cryptoorderbook.feature.market

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kwakwonjo.cryptoorderbook.core.domain.usecase.IsNetworkAvailableUseCase
import com.kwakwonjo.cryptoorderbook.core.domain.usecase.ObserveConnectivityUseCase
import com.kwakwonjo.cryptoorderbook.core.domain.usecase.ObserveMarketSummariesUseCase
import com.kwakwonjo.cryptoorderbook.core.model.ConnectivityStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MarketListViewModel @Inject constructor(
    private val observeMarketSummariesUseCase: ObserveMarketSummariesUseCase,
    observeConnectivityUseCase: ObserveConnectivityUseCase,
    isNetworkAvailableUseCase: IsNetworkAvailableUseCase,
) : ViewModel() {

    private var lastVisibleState: MarketListContract.UiState = MarketListContract.UiState.Loading

    private val refreshTrigger = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val connectivityStatus: StateFlow<ConnectivityStatus> = observeConnectivityUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = if (isNetworkAvailableUseCase()) {
                ConnectivityStatus.CONNECTED
            } else {
                ConnectivityStatus.DISCONNECTED
            },
        )

    val uiState: StateFlow<MarketListContract.UiState> = connectivityStatus
        .flatMapLatest { status ->
            if (status == ConnectivityStatus.DISCONNECTED) {
                emptyFlow()
            } else {
                refreshTrigger
                    .onStart { emit(Unit) }
                    .flatMapLatest {
                        observeMarketUiState(
                            emitLoading = lastVisibleState !is MarketListContract.UiState.Success,
                        )
                    }
            }
        }
        .onEach { state -> lastVisibleState = state }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = lastVisibleState,
        )

    fun retry() {
        refreshTrigger.tryEmit(Unit)
    }

    private fun observeMarketUiState(
        emitLoading: Boolean,
    ): Flow<MarketListContract.UiState> = flow {
        if (emitLoading) {
            emit(MarketListContract.UiState.Loading)
        }

        emitAll(
            observeMarketSummariesUseCase()
                .map { markets ->
                    val state: MarketListContract.UiState = MarketListContract.UiState.Success(markets)
                    state
                }
                .catch {
                    if (connectivityStatus.value == ConnectivityStatus.CONNECTED) {
                        emit(MarketListContract.UiState.Error)
                    }
                }
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
