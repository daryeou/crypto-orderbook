package com.kwakwonjo.cryptoorderbook.feature.orderbook

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun OrderBookRoute(
    onBack: () -> Unit,
    viewModel: OrderBookViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var previousStatus by remember { mutableStateOf(uiState.uiStatus) }

    OrderBookScreen(
        uiState = uiState,
        onBack = onBack,
        onRetry = viewModel::retry,
    )
}
