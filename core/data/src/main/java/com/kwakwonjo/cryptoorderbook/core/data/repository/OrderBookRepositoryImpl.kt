package com.kwakwonjo.cryptoorderbook.core.data.repository

import com.kwakwonjo.cryptoorderbook.core.domain.repository.OrderBookRepository
import com.kwakwonjo.cryptoorderbook.core.model.ConnectionState
import com.kwakwonjo.cryptoorderbook.core.model.OrderBook
import com.kwakwonjo.cryptoorderbook.core.model.OrderBookPayload
import com.kwakwonjo.cryptoorderbook.core.model.OrderBookUnit
import com.kwakwonjo.cryptoorderbook.core.model.TickerSnapshot
import com.kwakwonjo.cryptoorderbook.core.network.model.UpbitOrderBookFrame
import com.kwakwonjo.cryptoorderbook.core.network.model.UpbitSubscription
import com.kwakwonjo.cryptoorderbook.core.network.model.UpbitTickerFrame
import com.kwakwonjo.cryptoorderbook.core.network.model.UpbitWsFrame
import com.kwakwonjo.cryptoorderbook.core.network.websocket.UpbitWebSocketClient
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.runningFold

class OrderBookRepositoryImpl @Inject constructor(
    private val upbitWebSocketClient: UpbitWebSocketClient,
) : OrderBookRepository {

    override fun observeOrderBook(market: String): Flow<OrderBookPayload> {
        val initialPayload = OrderBookPayload(connectionState = ConnectionState.Connecting)

        return upbitWebSocketClient.observeUpbitStream(
            subscription = UpbitSubscription(market = market),
        ).runningFold(initialPayload) { current, frame ->
            current.reduce(frame)
        }.catch { throwable ->
            emit(
                OrderBookPayload(
                    connectionState = ConnectionState.Error,
                    errorMessage = throwable.message,
                )
            )
        }
    }

    private fun OrderBookPayload.reduce(frame: UpbitWsFrame): OrderBookPayload {
        return when (frame) {
            is UpbitOrderBookFrame -> copy(
                connectionState = ConnectionState.Connected,
                orderBook = OrderBook(
                    market = frame.market,
                    asks = frame.units.map { unit ->
                        OrderBookUnit(price = unit.askPrice, size = unit.askSize)
                    },
                    bids = frame.units.map { unit ->
                        OrderBookUnit(price = unit.bidPrice, size = unit.bidSize)
                    },
                    totalAskSize = frame.totalAskSize,
                    totalBidSize = frame.totalBidSize,
                    timestamp = frame.timestamp,
                ),
                errorMessage = null,
            )

            is UpbitTickerFrame -> copy(
                connectionState = ConnectionState.Connected,
                ticker = TickerSnapshot(
                    market = frame.market,
                    tradePrice = frame.tradePrice,
                    signedChangeRate = frame.signedChangeRate,
                    timestamp = frame.timestamp,
                ),
                errorMessage = null,
            )
        }
    }
}

