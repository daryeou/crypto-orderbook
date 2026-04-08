package com.kwakwonjo.cryptoorderbook.feature.orderbook

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class OrderBookNavKey(
    val market: String,
    val label: String,
) : NavKey
