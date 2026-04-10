package com.kwakwonjo.cryptoorderbook.feature.orderbook

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.kwakwonjo.cryptoorderbook.core.model.MarketType
import kotlinx.serialization.Serializable

@Serializable
data class OrderBookNavKey(
    val market: String,
    val marketType: MarketType,
    val koreanName: String,
) : NavKey

@Composable
fun OrderBookRoute(
    onBack: () -> Unit,
    viewModel: OrderBookViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    OrderBookScreen(
        uiState = uiState,
        onBack = onBack,
        onRetry = viewModel::retry,
    )
}
