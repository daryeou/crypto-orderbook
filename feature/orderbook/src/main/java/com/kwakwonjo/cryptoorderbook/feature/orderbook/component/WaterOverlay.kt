package com.kwakwonjo.cryptoorderbook.feature.orderbook.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.kwakwonjo.cryptoorderbook.core.designsystem.theme.LocalColors

@Composable
internal fun WaterOverlay(
    askSize: Double,
    bidSize: Double,
    modifier: Modifier = Modifier
) {
    val total = askSize + bidSize

    val askRatio = if (total > 0) (askSize / total).toFloat() else 0f
    val bidRatio = if (total > 0) (bidSize / total).toFloat() else 0f

    val infiniteTransition = rememberInfiniteTransition(label = "GlobalWave")

    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "WaveOffset"
    )

    // 물 애니메이션
    val animatedAskHeight by animateFloatAsState(targetValue = askRatio, animationSpec = tween(1000))
    val animatedBidHeight by animateFloatAsState(targetValue = bidRatio, animationSpec = tween(1000))

    val askColor = LocalColors.tradeUpRed.copy(alpha = 0.15f)
    val bidColor = LocalColors.tradeDownGreen.copy(alpha = 0.15f)

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val waveAmplitude = 12.dp.toPx() // 물결 치는 높이
        val waveFrequency = 0.005f // 물결 주폭

        // 상단 매도 물결 (Top -> Down)
        val askPath = Path().apply {
            moveTo(0f, 0f)
            lineTo(width, 0f)
            val fillHeight = height * animatedAskHeight * 0.4f // 최대 화면의 40%까지만 차오르게 제한

            for (x in width.toInt() downTo 0) {
                val y = fillHeight + (waveAmplitude * kotlin.math.sin(x * waveFrequency + waveOffset))
                lineTo(x.toFloat(), y)
            }
            close()
        }
        drawPath(path = askPath, color = askColor)

        // 하단 매수 물결 (Bottom -> Up)
        val bidPath = Path().apply {
            moveTo(0f, height)
            lineTo(width, height)
            val fillHeight = height - (height * animatedBidHeight * 0.4f)

            for (x in width.toInt() downTo 0) {
                val y = fillHeight + (waveAmplitude * kotlin.math.sin(x * waveFrequency - waveOffset))
                lineTo(x.toFloat(), y)
            }
            close()
        }
        drawPath(path = bidPath, color = bidColor)
    }
}