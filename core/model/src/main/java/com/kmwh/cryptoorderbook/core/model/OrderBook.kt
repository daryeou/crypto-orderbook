package com.kmwh.cryptoorderbook.core.model

data class OrderBook(
    val market: String,
    val asks: List<OrderBookUnit>,
    val bids: List<OrderBookUnit>,
    val totalAskSize: Double,
    val totalBidSize: Double,
    val timestamp: Long,
)

data class OrderBookUnit(
    val price: Double,
    val size: Double,
)

