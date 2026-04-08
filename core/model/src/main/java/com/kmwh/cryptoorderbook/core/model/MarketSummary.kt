package com.kmwh.cryptoorderbook.core.model

data class MarketSummary(
    val market: String,
    val koreanName: String,
    val englishName: String,
    val tradePrice: Double,
    val signedChangeRate: Double,
)

