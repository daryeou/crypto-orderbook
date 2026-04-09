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
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    var previousStatus by remember { mutableStateOf(uiState.value.uiStatus) }

    LaunchedEffect(uiState.value.uiStatus) {
        if (
            previousStatus == OrderBookContract.UiStatus.OFFLINE &&
            uiState.value.uiStatus != OrderBookContract.UiStatus.OFFLINE
        ) {
            viewModel.refresh()
        }

        previousStatus = uiState.value.uiStatus
    }

    OrderBookScreen(
        uiState = uiState.value,
        onBack = onBack,
        onRetry = viewModel::retry,
    )
}
