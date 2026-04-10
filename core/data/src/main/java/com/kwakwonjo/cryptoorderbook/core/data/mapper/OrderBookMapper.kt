package com.kwakwonjo.cryptoorderbook.core.data.mapper

import com.kwakwonjo.cryptoorderbook.core.domain.model.OrderBookEvent
import com.kwakwonjo.cryptoorderbook.core.network.model.UpbitOrderBookFrame
import com.kwakwonjo.cryptoorderbook.core.network.model.UpbitTickerFrame

internal fun UpbitOrderBookFrame.toDomain() = OrderBookEvent.OrderBook(
    market = market,
    asks = units.map { OrderBookEvent.OrderBook.OrderBookUnit(it.askPrice, it.askSize) },
    bids = units.map { OrderBookEvent.OrderBook.OrderBookUnit(it.bidPrice, it.bidSize) },
    totalAskSize = totalAskSize,
    totalBidSize = totalBidSize,
    timestamp = timestamp
)

internal fun UpbitTickerFrame.toDomain() = OrderBookEvent.TickerSnapshot(
    market = market,
    tradePrice = tradePrice,
    signedChangeRate = signedChangeRate,
    timestamp = timestamp
)