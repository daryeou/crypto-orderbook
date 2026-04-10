package com.kwakwonjo.cryptoorderbook.feature.orderbook

import com.kwakwonjo.cryptoorderbook.core.domain.model.OrderBookEvent
import com.kwakwonjo.cryptoorderbook.core.model.MarketType

sealed interface OrderBookContract {
    data class MarketInfo(
        val market: String,
        val marketType: MarketType,
        val koreanName: String,
    )

    data class OrderBookData(
        val orderBook: OrderBookEvent.OrderBook,
        val currentPrice: Double?,
        val signedChangeRate: Double?,
    )

    enum class UiStatus {
        IDLE,
        INITIAL_LOADING,
        SOCKET_ERROR
    }

    data class UiState(
        val marketInfo: MarketInfo,
        val orderBookData: OrderBookData?,
        val uiStatus: UiStatus,
    ) : OrderBookContract
}
