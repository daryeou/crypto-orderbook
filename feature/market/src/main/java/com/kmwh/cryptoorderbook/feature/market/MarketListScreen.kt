package com.kmwh.cryptoorderbook.feature.market

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kmwh.cryptoorderbook.core.model.MarketSummary
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketListScreen(
    uiState: MarketListUiState,
    onMarketClick: (market: String, marketLabel: String) -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Upbit KRW 마켓")
                }
            )
        }
    ) { innerPadding ->
        when (uiState) {
            MarketListUiState.Loading -> LoadingContent(Modifier.padding(innerPadding))
            is MarketListUiState.Error -> ErrorContent(
                message = uiState.message,
                onRetry = onRetry,
                modifier = Modifier.padding(innerPadding),
            )

            is MarketListUiState.Success -> MarketListContent(
                markets = uiState.markets,
                onMarketClick = onMarketClick,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(
            text = "마켓 정보를 불러오는 중입니다.",
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(
            onClick = onRetry,
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Text(text = "다시 시도")
        }
    }
}

@Composable
private fun MarketListContent(
    markets: List<MarketSummary>,
    onMarketClick: (market: String, marketLabel: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
    ) {
        items(
            items = markets,
            key = { it.market },
        ) { market ->
            MarketRow(
                market = market,
                onClick = {
                    onMarketClick(
                        market.market,
                        "${market.koreanName} (${market.market})",
                    )
                },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun MarketRow(
    market: MarketSummary,
    onClick: () -> Unit,
) {
    val changeColor = if (market.signedChangeRate >= 0) {
        Color(0xFFD32F2F)
    } else {
        Color(0xFF1976D2)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = market.koreanName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "${market.englishName} · ${market.market}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Card {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = market.tradePrice.toWonString(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = market.signedChangeRate.toPercentString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = changeColor,
                )
            }
        }
    }
}

private fun Double.toWonString(): String = NumberFormat.getNumberInstance(Locale.KOREA).format(this)

private fun Double.toPercentString(): String = String.format(Locale.KOREA, "%+.2f%%", this * 100)
