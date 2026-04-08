package com.kwakwonjo.cryptoorderbook.feature.market

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun MarketListRoute(
    onMarketClick: (market: String, marketLabel: String) -> Unit,
    viewModel: MarketListViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()

    MarketListScreen(
        uiState = uiState.value,
        onMarketClick = onMarketClick,
        onRetry = viewModel::retry,
    )
}
