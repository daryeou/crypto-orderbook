package com.kwakwonjo.cryptoorderbook.feature.orderbook

import com.kwakwonjo.cryptoorderbook.core.model.OrderBook

sealed interface OrderBookContract {
    data class Meta(
        val market: String,
        val marketLabel: String,
    )

    enum class ErrorType {
        NETWORK,
        SOCKET,
    }

    sealed interface UiState : OrderBookContract {
        val meta: Meta

        data class Loading(
            override val meta: Meta,
        ) : UiState

        data class Error(
            override val meta: Meta,
            val type: ErrorType,
        ) : UiState {
            val canRetry: Boolean
                get() = type == ErrorType.SOCKET
        }

        data class Success(
            override val meta: Meta,
            val orderBook: OrderBook,
            val currentPrice: Double?,
            val signedChangeRate: Double?,
        ) : UiState
    }
}
