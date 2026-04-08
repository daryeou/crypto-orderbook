package com.kwakwonjo.cryptoorderbook.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface AppDestination : NavKey {
    @Serializable
    data object MarketList : AppDestination
}

