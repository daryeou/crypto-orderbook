package com.kwakwonjo.cryptoorderbook.core.model

data class OrderBookPayload(
    val connectionState: ConnectionState,
    val orderBook: OrderBook? = null,
    val ticker: TickerSnapshot? = null,
    val errorMessage: String? = null,
)

