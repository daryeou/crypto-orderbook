package com.kwakwonjo.cryptoorderbook.feature.market

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.kwakwonjo.cryptoorderbook.core.model.MarketType
import kotlinx.serialization.Serializable

@Serializable
data object MarketListNavKey : NavKey

@Composable
fun MarketListRoute(
    onMarketClick: (market: String, marketType: MarketType, koreanName: String) -> Unit,
    viewModel: MarketListViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()

    MarketListScreen(
        uiState = uiState.value,
        onMarketClick = onMarketClick,
        onRetry = viewModel::retry,
    )
}
