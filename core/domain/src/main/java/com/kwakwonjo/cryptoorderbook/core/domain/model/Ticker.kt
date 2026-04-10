package com.kwakwonjo.cryptoorderbook.core.domain.model

data class Ticker(
    val market: String,
    val tradePrice: Double,
    val signedChangeRate: Double,
)