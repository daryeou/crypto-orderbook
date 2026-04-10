package com.kwakwonjo.cryptoorderbook.core.model

data class Ticker(
    val market: String,
    val tradePrice: Double,
    val signedChangeRate: Double,
)