package com.kwakwonjo.cryptoorderbook.feature.orderbook

import androidx.compose.runtime.Stable
import com.kwakwonjo.cryptoorderbook.core.domain.model.OrderBookEvent
import com.kwakwonjo.cryptoorderbook.core.model.MarketType

internal sealed interface OrderBookContract {
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

    sealed interface Error

    sealed interface UiStatus {
        data object Idle: UiStatus
        data object InitialLoading: UiStatus
        data object SocketError: UiStatus, Error
        data object Offline: UiStatus, Error
    }

    @Stable
    data class UiState(
        val marketInfo: MarketInfo,
        val orderBookData: OrderBookData?,
        val uiStatus: UiStatus,
    ) : OrderBookContract {
        val isError: Boolean
            get() = uiStatus is Error
    }
}
