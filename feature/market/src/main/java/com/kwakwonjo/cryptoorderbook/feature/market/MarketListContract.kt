package com.kwakwonjo.cryptoorderbook.feature.market

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.kwakwonjo.cryptoorderbook.core.model.Market
import com.kwakwonjo.cryptoorderbook.core.model.Ticker

internal sealed interface MarketListContract {

    @Immutable
    data class MarketItem(
        val market: Market,
        val ticker: Ticker,
    )

    enum class UiStatus {
        IDLE,
        INITIAL_LOADING,
        OFFLINE,
        ERROR
    }

    @Stable
    data class UiState(
        val items: List<MarketItem>,
        val uiStatus: UiStatus
    )
}
