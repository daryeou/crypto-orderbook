package com.kwakwonjo.cryptoorderbook.core.model

enum class MarketType(val isVisible: Boolean) {
    KRW(true),
    BTC(true),
    USDT(true),
    UNKNOWN(false);
}