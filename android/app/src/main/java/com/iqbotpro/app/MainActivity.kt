package com.iqbotpro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.iqbotpro.app.data.security.SecureStorageManager
import com.iqbotpro.app.ui.MainApp
import com.iqbotpro.app.ui.theme.IQBotProTheme

class MainActivity : ComponentActivity() {

    private lateinit var secureStorage: SecureStorageManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        secureStorage = SecureStorageManager(applicationContext)

        setContent {
            IQBotProTheme {
                MainApp(
                    secureStorage = secureStorage
                )
            }
        }
    }
}
