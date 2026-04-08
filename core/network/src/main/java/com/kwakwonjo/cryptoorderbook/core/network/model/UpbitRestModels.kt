package com.kwakwonjo.cryptoorderbook.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpbitMarketResponse(
    val market: String,
    @SerialName("korean_name") val koreanName: String,
    @SerialName("english_name") val englishName: String,
)

@Serializable
data class UpbitTickerResponse(
    val market: String,
    @SerialName("trade_price") val tradePrice: Double,
    @SerialName("signed_change_rate") val signedChangeRate: Double,
)


