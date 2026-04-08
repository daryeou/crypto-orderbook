package com.kwakwonjo.cryptoorderbook.feature.market

import com.kwakwonjo.cryptoorderbook.core.model.MarketSummary

sealed interface MarketListContract {
    sealed interface UiState : MarketListContract {
        data object Loading : UiState

        data object Error : UiState

        data class Success(
            val markets: List<MarketSummary>,
        ) : UiState
    }
}
