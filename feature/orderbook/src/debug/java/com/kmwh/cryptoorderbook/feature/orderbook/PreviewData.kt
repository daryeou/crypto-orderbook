package com.kmwh.cryptoorderbook.feature.orderbook

import com.kmwh.cryptoorderbook.core.model.ConnectionState
import com.kmwh.cryptoorderbook.core.model.OrderBook
import com.kmwh.cryptoorderbook.core.model.OrderBookUnit

internal object PreviewData {
    val orderBookState = OrderBookUiState(
        market = "KRW-BTC",
        marketLabel = "비트코인 (KRW-BTC)",
        connectionState = ConnectionState.Connected,
        currentPrice = 148_956_000.0,
        signedChangeRate = 0.0031,
        orderBook = OrderBook(
            market = "KRW-BTC",
            asks = listOf(
                OrderBookUnit(148_960_000.0, 0.13),
                OrderBookUnit(148_970_000.0, 0.27),
                OrderBookUnit(148_980_000.0, 0.44),
            ),
            bids = listOf(
                OrderBookUnit(148_950_000.0, 0.31),
                OrderBookUnit(148_940_000.0, 0.22),
                OrderBookUnit(148_930_000.0, 0.12),
            ),
            totalAskSize = 5.3,
            totalBidSize = 4.8,
            timestamp = 1_746_601_573_804L,
        ),
    )
}

