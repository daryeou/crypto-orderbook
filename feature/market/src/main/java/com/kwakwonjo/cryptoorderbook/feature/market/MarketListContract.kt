package com.kwakwonjo.cryptoorderbook.feature.market

import com.kwakwonjo.cryptoorderbook.core.model.MarketType

sealed interface MarketListContract {
    data class MarketItem(
        val market: String,
        val marketType: MarketType,
        val koreanName: String,
        val englishName: String,
        val tradePrice: Double,
        val signedChangeRate: Double,
    )

    enum class UiStatus {
        IDLE,
        INITIAL_LOADING,
        ERROR
    }

    data class UiState(
        val items: List<MarketItem>,
        val uiStatus: UiStatus
    )
}
