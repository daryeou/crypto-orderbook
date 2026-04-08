package com.kmwh.cryptoorderbook.core.model

data class TickerSnapshot(
    val market: String,
    val tradePrice: Double,
    val signedChangeRate: Double,
    val timestamp: Long,
)

