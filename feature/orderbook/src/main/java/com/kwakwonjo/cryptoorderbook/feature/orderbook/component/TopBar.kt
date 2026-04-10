package com.kwakwonjo.cryptoorderbook.feature.orderbook.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.kwakwonjo.cryptoorderbook.core.designsystem.component.HorizontalSpacer
import com.kwakwonjo.cryptoorderbook.core.ui.component.MarketIcon
import com.kwakwonjo.cryptoorderbook.feature.orderbook.R

@Composable
internal fun TopBar(
    market: String,
    koreanName: String,
    isConnected: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MarketIcon(
            market = market,
            size = 40.dp
        )

        HorizontalSpacer(8.dp)

        // 2. 코인명 및 마켓 정보
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = koreanName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = market,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 3. 온라인/오프라인 상태 표시등
        StatusIndicator(isConnected = isConnected)

        HorizontalSpacer(8.dp)
    }
}

private enum class NetworkState {
    ONLINE,
    OFFLINE
}

private fun NetworkState.color() = when(this) {
    NetworkState.ONLINE -> Color(0xFF4CAF50)
    NetworkState.OFFLINE -> Color(0xFFFFCA28)
}

private fun NetworkState.text() = when(this) {
    NetworkState.ONLINE -> R.string.network_online_tag
    NetworkState.OFFLINE -> R.string.network_offline_tag
}

@Composable
private fun StatusIndicator(isConnected: Boolean) {
    val state = if (isConnected) NetworkState.ONLINE else NetworkState.OFFLINE

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = state.color().copy(alpha = 0.1f),
        border = BorderStroke(1.dp, state.color().copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(state.color(), CircleShape)
            )
            HorizontalSpacer(4.dp)
            Text(
                text = stringResource(state.text()),
                style = MaterialTheme.typography.labelSmall,
                color = state.color(),
                fontWeight = FontWeight.Bold
            )
        }
    }
}