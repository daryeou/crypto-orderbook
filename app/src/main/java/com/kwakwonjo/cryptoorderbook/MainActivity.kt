package com.kwakwonjo.cryptoorderbook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kwakwonjo.cryptoorderbook.core.designsystem.theme.CryptoAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CryptoAppTheme {
                CryptoOrderBookApp(
                    onFinishRequest = ::finish,
                )
            }
        }
    }
}


