package com.kmwh.cryptoorderbook.feature.market

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun MarketListRoute(
    onMarketClick: (market: String, marketLabel: String) -> Unit,
    viewModel: MarketListViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.startPolling()
    }

    DisposableEffect(viewModel) {
        onDispose {
            viewModel.stopPolling()
        }
    }

    MarketListScreen(
        uiState = uiState.value,
        onMarketClick = onMarketClick,
        onRetry = viewModel::retry,
    )
}
