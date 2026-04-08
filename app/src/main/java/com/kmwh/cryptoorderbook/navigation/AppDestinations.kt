package com.kmwh.cryptoorderbook.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface AppDestination : NavKey {
    @Serializable
    data object MarketList : AppDestination

    @Serializable
    data class OrderBook(
        val market: String,
        val label: String,
    ) : AppDestination
}
