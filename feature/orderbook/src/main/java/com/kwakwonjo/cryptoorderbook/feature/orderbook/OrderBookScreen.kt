package com.kwakwonjo.cryptoorderbook.feature.orderbook

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kwakwonjo.cryptoorderbook.core.domain.model.Market
import com.kwakwonjo.cryptoorderbook.core.domain.model.OrderBookEvent
import com.kwakwonjo.cryptoorderbook.core.model.MarketType
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderBookScreen(
    uiState: OrderBookContract.UiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = uiState.marketInfo.koreanName.ifBlank {
                            stringResource(R.string.orderbook_title_fallback)
                        },
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.orderbook_back),
                        )
                    }
                },
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            OrderBookContent(
                uiState = uiState,
                modifier = Modifier.fillMaxSize(),
            )

            if (uiState.uiStatus == OrderBookContract.UiStatus.INITIAL_LOADING) {
                OrderBookLoadingOverlay(
                    modifier = Modifier.fillMaxSize(),
                )
            }

            if (uiState.uiStatus == OrderBookContract.UiStatus.SOCKET_ERROR) {
                OrderBookRetryOverlay(
                    onRetry = onRetry,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun OrderBookLoadingOverlay(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.orderbook_loading),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun OrderBookRetryOverlay(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.orderbook_error_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.orderbook_error_message_socket),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onRetry,
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Text(text = stringResource(R.string.orderbook_retry))
            }
        }
    }
}

@Composable
private fun OrderBookContent(
    uiState: OrderBookContract.UiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
    ) {
        AskSection(
            orderBook = uiState.orderBookData?.orderBook,
            marketType = uiState.marketInfo.marketType,
        )
        CurrentPriceCard(
            currentPrice = uiState.orderBookData?.currentPrice,
            signedChangeRate = uiState.orderBookData?.signedChangeRate,
            marketType = uiState.marketInfo.marketType,
        )
        BidSection(
            orderBook = uiState.orderBookData?.orderBook,
            marketType = uiState.marketInfo.marketType,
        )
    }
}

@Composable
private fun AskSection(
    orderBook: OrderBookEvent.OrderBook?,
    marketType: MarketType,
) {
    Text(
        text = stringResource(R.string.orderbook_ask_section),
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )

    val asks = orderBook?.asks.orEmpty()
    val maxAskSize = asks.maxOfOrNull { it.size } ?: 1.0

    if (asks.isEmpty()) {
        repeat(3) {
            PlaceholderOrderBookRow()
        }
    } else {
        asks.reversed().forEach { unit ->
            OrderBookRow(
                unit = unit,
                maxSize = maxAskSize,
                label = "ASK",
                priceColor = Color(0xFFD32F2F),
                backgroundColor = Color(0xFFFFEBEE),
                marketType = marketType,
            )
        }
    }
}

@Composable
private fun CurrentPriceCard(
    currentPrice: Double?,
    signedChangeRate: Double?,
    marketType: MarketType,
) {
    val changeColor = if ((signedChangeRate ?: 0.0) >= 0) {
        Color(0xFFD32F2F)
    } else {
        Color(0xFF1976D2)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.orderbook_current_price),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = currentPrice?.toPriceString(marketType) ?: "--",
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            if (signedChangeRate != null) {
                Text(
                    text = String.format(Locale.KOREA, "%+.2f%%", signedChangeRate * 100),
                    modifier = Modifier.padding(top = 4.dp),
                    color = changeColor,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun BidSection(
    orderBook: OrderBookEvent.OrderBook?,
    marketType: MarketType,
) {
    Text(
        text = stringResource(R.string.orderbook_bid_section),
        modifier = Modifier.padding(bottom = 8.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )

    val bids = orderBook?.bids.orEmpty()
    val maxBidSize = bids.maxOfOrNull { it.size } ?: 1.0

    if (bids.isEmpty()) {
        repeat(3) {
            PlaceholderOrderBookRow()
        }
    } else {
        bids.forEach { unit ->
            OrderBookRow(
                unit = unit,
                maxSize = maxBidSize,
                label = "BID",
                priceColor = Color(0xFF1976D2),
                backgroundColor = Color(0xFFE3F2FD),
                marketType = marketType,
            )
        }
    }
}

@Composable
private fun PlaceholderOrderBookRow() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "--",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "--",
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun OrderBookRow(
    unit: OrderBookEvent.OrderBook.OrderBookUnit,
    maxSize: Double,
    label: String,
    priceColor: Color,
    backgroundColor: Color,
    marketType: MarketType,
) {
    val widthFraction = (unit.size / maxSize).toFloat().coerceIn(0.15f, 1f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(widthFraction)
                    .align(if (label == "ASK") Alignment.CenterEnd else Alignment.CenterStart)
                    .background(backgroundColor),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = unit.price.toPriceString(marketType),
                    color = priceColor,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = unit.size.toVolumeString(),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private fun Double.toPriceString(marketType: MarketType): String = when (marketType) {
    MarketType.KRW -> NumberFormat.getNumberInstance(Locale.KOREA).format(this)
    else -> DECIMAL_PRICE_FORMAT.format(this)
}

private fun Double.toVolumeString(): String = String.format(Locale.KOREA, "%.4f", this)

private val DECIMAL_PRICE_FORMAT = DecimalFormat("#,##0.#########")
