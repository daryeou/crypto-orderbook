package com.kwakwonjo.cryptoorderbook.feature.orderbook

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun OrderBookRoute(
    onBack: () -> Unit,
    viewModel: OrderBookViewModel,
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()

    OrderBookScreen(
        uiState = uiState.value,
        onBack = onBack,
        onRetry = viewModel::retry,
    )
}
