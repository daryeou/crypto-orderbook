package com.kwakwonjo.cryptoorderbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kwakwonjo.cryptoorderbook.core.domain.usecase.IsNetworkAvailableUseCase
import com.kwakwonjo.cryptoorderbook.core.domain.usecase.ObserveConnectivityUseCase
import com.kwakwonjo.cryptoorderbook.core.model.ConnectivityStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class AppRootViewModel @Inject constructor(
    observeConnectivityUseCase: ObserveConnectivityUseCase,
    isNetworkAvailableUseCase: IsNetworkAvailableUseCase,
) : ViewModel() {

    val connectivityStatus: StateFlow<ConnectivityStatus> = observeConnectivityUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = if (isNetworkAvailableUseCase()) {
                ConnectivityStatus.CONNECTED
            } else {
                ConnectivityStatus.DISCONNECTED
            },
        )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
