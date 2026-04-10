package com.kwakwonjo.cryptoorderbook.feature.orderbook.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kwakwonjo.cryptoorderbook.core.designsystem.component.HorizontalSpacer
import com.kwakwonjo.cryptoorderbook.core.designsystem.component.VerticalSpacer
import com.kwakwonjo.cryptoorderbook.core.designsystem.theme.LocalColors
import com.kwakwonjo.cryptoorderbook.core.domain.model.OrderBookEvent
import com.kwakwonjo.cryptoorderbook.core.model.MarketType
import com.kwakwonjo.cryptoorderbook.feature.orderbook.OrderBookContract
import com.kwakwonjo.cryptoorderbook.feature.orderbook.OrderBookUnit
import com.kwakwonjo.cryptoorderbook.feature.orderbook.R
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

@Composable
internal fun OrderBookContent(
    uiState: OrderBookContract.UiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    val asks = uiState.orderBookData?.orderBook?.asks.orEmpty()
    val bids = uiState.orderBookData?.orderBook?.bids.orEmpty()

    // 막대기 길이를 매도/매수 전체 기준으로 스케일링
    val maxVolume = (asks + bids).maxOfOrNull { it.size } ?: 1.0

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
    ) {
        VerticalSpacer(8.dp)

        // 매도 (Asks)
        if (asks.isEmpty()) {
            repeat(OrderBookUnit) { PlaceholderOrderBookRow() }
        } else {
            // 높은 가격이 위에 오도록 역순 배치
            asks.reversed().forEach { unit ->
                OrderBookRow(
                    unit = unit,
                    maxSize = maxVolume,
                    isAsk = true,
                    marketType = uiState.marketInfo.marketType,
                )
            }
        }

        // 최신 실행가 및 상태
        CurrentPriceDivider(
            currentPrice = uiState.orderBookData?.currentPrice,
            uiStatus = uiState.uiStatus,
            marketType = uiState.marketInfo.marketType,
            onRetry = onRetry
        )

        // 매수 (Bids)
        if (bids.isEmpty()) {
            repeat(OrderBookUnit) { PlaceholderOrderBookRow() }
        } else {
            bids.forEach { unit ->
                OrderBookRow(
                    unit = unit,
                    maxSize = maxVolume,
                    isAsk = false,
                    marketType = uiState.marketInfo.marketType,
                )
            }
        }

        VerticalSpacer(24.dp)
    }
}

@Composable
private fun CurrentPriceDivider(
    currentPrice: Double?,
    uiStatus: OrderBookContract.UiStatus,
    marketType: MarketType,
    onRetry: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onRetry, modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.orderbook_current_price),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${marketType.name} ${currentPrice?.toPriceString(marketType) ?: "--"}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        HorizontalSpacer(24.dp)
    }
}

@Composable
private fun ColumnScope.OrderBookRow(
    unit: OrderBookEvent.OrderBook.OrderBookUnit,
    maxSize: Double,
    isAsk: Boolean,
    marketType: MarketType,
) {
    val priceColor = if (isAsk) LocalColors.tradeUpRed else LocalColors.tradeDownGreen
    val barColor = priceColor.copy(alpha = 0.2f)

    val fraction = (unit.size / maxSize).toFloat()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 가격
        Text(
            text = unit.price.toPriceString(marketType),
            modifier = Modifier.weight(1f),
            color = priceColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )

        // 호가량 막대
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .align(Alignment.CenterEnd)
                    .background(barColor, RoundedCornerShape(2.dp))
            )
        }

        // 수량
        Text(
            text = unit.size.toVolumeString(),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun PlaceholderOrderBookRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("--", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Box(modifier = Modifier.weight(1f))
        Text(
            "--",
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun Double.toPriceString(marketType: MarketType): String = when (marketType) {
    MarketType.KRW -> NumberFormat.getNumberInstance(Locale.KOREA).format(this)
    else -> DECIMAL_PRICE_FORMAT.format(this)
}

private fun Double.toVolumeString(): String = String.format(Locale.KOREA, "%.4f", this)

private val DECIMAL_PRICE_FORMAT = DecimalFormat("#,##0.#########")
