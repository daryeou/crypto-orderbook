package com.kwakwonjo.cryptoorderbook.core.model

data class TickerSnapshot(
    val market: String,
    val tradePrice: Double,
    val signedChangeRate: Double,
    val timestamp: Long,
)


