package com.kwakwonjo.cryptoorderbook.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class UpbitSubscription(
    val market: String,
    val orderbookUnit: Int = 15,
)

sealed interface UpbitWsFrame

data class UpbitOrderBookFrame(
    val market: String,
    val totalAskSize: Double,
    val totalBidSize: Double,
    val units: List<UpbitOrderBookUnitFrame>,
    val timestamp: Long,
) : UpbitWsFrame

data class UpbitOrderBookUnitFrame(
    val askPrice: Double,
    val bidPrice: Double,
    val askSize: Double,
    val bidSize: Double,
)

data class UpbitTickerFrame(
    val market: String,
    val tradePrice: Double,
    val signedChangeRate: Double,
    val timestamp: Long,
) : UpbitWsFrame

@Serializable
internal data class UpbitOrderBookMessage(
    val type: String,
    val code: String,
    @SerialName("total_ask_size") val totalAskSize: Double,
    @SerialName("total_bid_size") val totalBidSize: Double,
    @SerialName("orderbook_units") val orderbookUnits: List<UpbitOrderBookUnitMessage>,
    val timestamp: Long,
)

@Serializable
internal data class UpbitOrderBookUnitMessage(
    @SerialName("ask_price") val askPrice: Double,
    @SerialName("bid_price") val bidPrice: Double,
    @SerialName("ask_size") val askSize: Double,
    @SerialName("bid_size") val bidSize: Double,
)

@Serializable
internal data class UpbitTickerMessage(
    val type: String,
    val code: String,
    @SerialName("trade_price") val tradePrice: Double,
    @SerialName("signed_change_rate") val signedChangeRate: Double,
    val timestamp: Long,
)


