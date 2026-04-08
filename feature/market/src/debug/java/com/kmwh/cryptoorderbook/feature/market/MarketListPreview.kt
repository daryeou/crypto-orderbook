package com.kmwh.cryptoorderbook.feature.market

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview(showBackground = true)
@Composable
private fun MarketListPreview() {
    MarketListScreen(
        uiState = MarketListUiState.Success(PreviewData.marketSummaries),
        onMarketClick = { _, _ -> },
        onRetry = {},
    )
}
