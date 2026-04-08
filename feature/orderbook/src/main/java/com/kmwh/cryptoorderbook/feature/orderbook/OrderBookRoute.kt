package com.kmwh.cryptoorderbook.feature.orderbook

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun OrderBookRoute(
    market: String,
    marketLabel: String,
    onBack: () -> Unit,
    viewModel: OrderBookViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(market, marketLabel) {
        viewModel.start(market, marketLabel)
    }

    DisposableEffect(viewModel) {
        onDispose {
            viewModel.stop()
        }
    }

    OrderBookScreen(
        uiState = uiState.value,
        onBack = onBack,
        onRetry = viewModel::retry,
    )
}
