package com.kwakwonjo.cryptoorderbook.core.domain.model

import com.kwakwonjo.cryptoorderbook.core.model.ConnectionState

data class OrderBookEvent(
    val connectionState: ConnectionState,
    val orderBook: OrderBook? = null,
    val ticker: TickerSnapshot? = null,
    val errorMessage: String? = null,
) {
    data class OrderBook(
        val market: String,
        val asks: List<OrderBookUnit>,
        val bids: List<OrderBookUnit>,
        val totalAskSize: Double,
        val totalBidSize: Double,
        val timestamp: Long,
    ) {
        data class OrderBookUnit(
            val price: Double,
            val size: Double,
        )
    }

    data class TickerSnapshot(
        val market: String,
        val tradePrice: Double,
        val signedChangeRate: Double,
        val timestamp: Long,
    )
}