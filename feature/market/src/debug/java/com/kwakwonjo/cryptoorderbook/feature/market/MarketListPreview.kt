package com.kwakwonjo.cryptoorderbook.feature.market

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kwakwonjo.cryptoorderbook.core.designsystem.theme.CryptoAppTheme

@Preview(name = "Idle", showBackground = true, backgroundColor = 0xFF141416)
@Composable
private fun MarketListIdlePreview() {
    MarketListPreview(
        uiState = MarketListContract.UiState(
            items = PreviewData.marketItems,
            uiStatus = MarketListContract.UiStatus.IDLE,
        ),
    )
}

@Preview(name = "Loading", showBackground = true, backgroundColor = 0xFF141416)
@Composable
private fun MarketListLoadingPreview() {
    MarketListPreview(
        uiState = MarketListContract.UiState(
            items = emptyList(),
            uiStatus = MarketListContract.UiStatus.INITIAL_LOADING,
        ),
    )
}

@Preview(name = "Error", showBackground = true, backgroundColor = 0xFF141416)
@Composable
private fun MarketListErrorPreview() {
    MarketListPreview(
        uiState = MarketListContract.UiState(
            items = emptyList(),
            uiStatus = MarketListContract.UiStatus.ERROR,
        ),
    )
}

@Composable
private fun MarketListPreview(uiState: MarketListContract.UiState) {
    CryptoAppTheme {
        MarketListScreen(
            uiState = uiState,
            onMarketClick = { _, _, _ -> },
            onRetry = {},
        )
    }
}
