package com.kwakwonjo.cryptoorderbook.feature.orderbook

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun OrderBookRoute(
    onBack: () -> Unit,
    viewModel: OrderBookViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()

    OrderBookScreen(
        uiState = uiState.value,
        onBack = onBack,
        onRetry = viewModel::retry,
    )
}
