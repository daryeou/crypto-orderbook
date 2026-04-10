package com.kwakwonjo.cryptoorderbook.core.domain.model

import com.kwakwonjo.cryptoorderbook.core.model.MarketType

data class Market(
    val market: String,
    val koreanName: String,
    val englishName: String,
) {
    val marketType = when {
        market.startsWith("KRW-") -> MarketType.KRW
        market.startsWith("BTC-") -> MarketType.BTC
        market.startsWith("USDT-") -> MarketType.USDT
        else -> MarketType.UNKNOWN
    }
}


