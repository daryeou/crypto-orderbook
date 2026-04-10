package com.kwakwonjo.cryptoorderbook.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage

@Composable
fun MarketIcon(
    market: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val symbol = remember(market) {
        market.split("-").getOrElse(1) { market }
    }

    val iconUrl = remember(symbol) {
        "https://cdn.jsdelivr.net/gh/prasangapokharel/crypto-icons@v1.0.0/binance/${symbol.uppercase()}.png"
    }

    SubcomposeAsyncImage(
        model = iconUrl,
        contentDescription = "$symbol market icon",
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        loading = {
            IconPlaceholder(symbol)
        },
        error = {
            IconPlaceholder(symbol)
        },
    )
}

@Composable
private fun IconPlaceholder(symbol: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol.take(1).uppercase().ifEmpty { "-" },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
    }
}