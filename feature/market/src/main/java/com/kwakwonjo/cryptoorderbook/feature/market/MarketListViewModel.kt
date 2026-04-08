package com.kwakwonjo.cryptoorderbook.feature.market

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kwakwonjo.cryptoorderbook.core.domain.usecase.ObserveMarketSummariesUseCase
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MarketListViewModel @Inject constructor(
    private val observeMarketSummariesUseCase: ObserveMarketSummariesUseCase,
) : ViewModel() {

    private val refreshTrigger = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val uiState: StateFlow<MarketListContract.UiState> = refreshTrigger
        .onStart { emit(Unit) }
        .flatMapLatest { observeMarketUiState() }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = MarketListContract.UiState.Loading,
        )

    fun retry() {
        refreshTrigger.tryEmit(Unit)
    }

    private fun observeMarketUiState(): Flow<MarketListContract.UiState> {
        return observeMarketSummariesUseCase()
            .map { markets ->
                val state: MarketListContract.UiState = MarketListContract.UiState.Success(markets)
                state
            }
            .onStart {
                emit(MarketListContract.UiState.Loading)
            }
            .catch {
                emit(MarketListContract.UiState.Error)
            }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
