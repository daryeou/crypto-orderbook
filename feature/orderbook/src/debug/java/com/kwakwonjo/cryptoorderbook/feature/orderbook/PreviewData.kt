package com.kwakwonjo.cryptoorderbook.feature.orderbook

import com.kwakwonjo.cryptoorderbook.core.domain.model.OrderBookEvent
import com.kwakwonjo.cryptoorderbook.core.model.MarketType

internal object PreviewData {
    val orderBookState = OrderBookContract.UiState(
        marketInfo = OrderBookContract.MarketInfo(
            market = "KRW-BTC",
            marketType = MarketType.KRW,
            koreanName = "Bitcoin (KRW-BTC)",
        ),
        orderBookData = OrderBookContract.OrderBookData(
            orderBook = OrderBookEvent.OrderBook(
                market = "KRW-BTC",
                asks = listOf(
                    OrderBookEvent.OrderBook.OrderBookUnit(148_960_000.0, 0.13),
                    OrderBookEvent.OrderBook.OrderBookUnit(148_970_000.0, 0.27),
                    OrderBookEvent.OrderBook.OrderBookUnit(148_980_000.0, 0.44),
                ),
                bids = listOf(
                    OrderBookEvent.OrderBook.OrderBookUnit(148_950_000.0, 0.31),
                    OrderBookEvent.OrderBook.OrderBookUnit(148_940_000.0, 0.22),
                    OrderBookEvent.OrderBook.OrderBookUnit(148_930_000.0, 0.12),
                ),
                totalAskSize = 5.3,
                totalBidSize = 4.8,
                timestamp = 1_746_601_573_804L,
            ),
            currentPrice = 148_956_000.0,
            signedChangeRate = 0.0031,
        ),
        uiStatus = OrderBookContract.UiStatus.IDLE,
    )
}
