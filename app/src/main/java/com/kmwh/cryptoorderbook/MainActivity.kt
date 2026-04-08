package com.kmwh.cryptoorderbook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.kmwh.cryptoorderbook.navigation.CryptoOrderBookNavHost
import com.kmwh.cryptoorderbook.theme.CryptoOrderBookTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            CryptoOrderBookTheme {
                CryptoOrderBookNavHost()
            }
        }
    }
}

