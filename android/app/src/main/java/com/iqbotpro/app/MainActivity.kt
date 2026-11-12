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
            Python.start(AndroidPlatform(applicationContext))
        }

        val python = Python.getInstance()
        val osModule = python.getModule("os")
        val environ = requireNotNull(osModule.get("environ")) {
            "Python os.environ is unavailable"
        }
        environ.callAttr("__setitem__", "IQBOT_DATA_DIR", filesDir.absolutePath)

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
