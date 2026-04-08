package com.kmwh.cryptoorderbook.feature.orderbook

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun OrderBookPreview() {
    OrderBookScreen(
        uiState = PreviewData.orderBookState,
        onBack = {},
        onRetry = {},
    )
}
