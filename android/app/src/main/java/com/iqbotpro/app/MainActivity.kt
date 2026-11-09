package com.iqbotpro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.iqbotpro.app.data.network.BotBridgeClient
import com.iqbotpro.app.data.security.SecureStorageManager
import com.iqbotpro.app.ui.MainApp
import com.iqbotpro.app.ui.theme.IQBotProTheme

class MainActivity : ComponentActivity() {

    private lateinit var secureStorage: SecureStorageManager
    private lateinit var bridgeClient: BotBridgeClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize secure storage and bot network client
        secureStorage = SecureStorageManager(applicationContext)
        bridgeClient = BotBridgeClient()

        setContent {
            IQBotProTheme {
                MainApp(
                    bridgeClient = bridgeClient,
                    secureStorage = secureStorage
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bridgeClient.disconnect()
    }
}
