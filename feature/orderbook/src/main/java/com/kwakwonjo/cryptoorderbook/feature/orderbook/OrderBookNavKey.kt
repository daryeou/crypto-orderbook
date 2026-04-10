package com.kwakwonjo.cryptoorderbook.feature.orderbook

import androidx.navigation3.runtime.NavKey
import com.kwakwonjo.cryptoorderbook.core.model.MarketType
import kotlinx.serialization.Serializable

@Serializable
data class OrderBookNavKey(
    val market: String,
    val marketType: MarketType,
    val koreanName: String,
) : NavKey
