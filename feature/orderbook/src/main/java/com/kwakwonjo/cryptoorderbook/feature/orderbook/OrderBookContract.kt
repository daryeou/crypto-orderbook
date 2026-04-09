package com.kwakwonjo.cryptoorderbook.feature.orderbook

import com.kwakwonjo.cryptoorderbook.core.model.OrderBook

sealed interface OrderBookContract {
    data class Meta(
        val market: String,
        val marketLabel: String,
    )

    data class Content(
        val orderBook: OrderBook,
        val currentPrice: Double?,
        val signedChangeRate: Double?,
    )

    enum class UiStatus {
        IDLE,
        INITIAL_LOADING,
        SOCKET_ERROR,
        OFFLINE,
    }

    data class UiState(
        val meta: Meta,
        val content: Content?,
        val uiStatus: UiStatus,
    ) : OrderBookContract
}
