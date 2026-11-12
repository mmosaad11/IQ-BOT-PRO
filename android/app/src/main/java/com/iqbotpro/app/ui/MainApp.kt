package com.iqbotpro.app.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.iqbotpro.app.R
import com.iqbotpro.app.data.model.BotState
import com.iqbotpro.app.data.model.TradeItem
import com.iqbotpro.app.data.network.PythonBotEngine
import com.iqbotpro.app.data.security.SecureStorageManager
import com.iqbotpro.app.service.BotForegroundService
import com.iqbotpro.app.ui.screens.*
import com.iqbotpro.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

enum class AppTab(val icon: ImageVector, val labelRes: Int) {
    DASHBOARD(Icons.Default.Dashboard, R.string.tab_dashboard),
    STRATEGY(Icons.Default.Equalizer, R.string.tab_strategy),
    HISTORY(Icons.Default.History, R.string.tab_history),
    SETTINGS(Icons.Default.Settings, R.string.tab_settings)
}

@Composable
fun MainApp(secureStorage: SecureStorageManager) {

    val context = LocalContext.current
    val engine = remember { PythonBotEngine() }

    var currentTab by remember { mutableStateOf(AppTab.DASHBOARD) }
    var currentLanguage by remember {
        mutableStateOf(secureStorage.getLanguage())
    }
    var selectedStrategyMode by remember { mutableStateOf("both") }

    var botState by remember { mutableStateOf(BotState.IDLE) }
    var botMessage by remember { mutableStateOf("غير شغال") }
    var balance by remember { mutableStateOf<Double?>(null) }
    var connected by remember { mutableStateOf(false) }
    var accountMode by remember { mutableStateOf<String?>(null) }
    var dailyProfit by remember { mutableStateOf(0.0) }
    var trades by remember { mutableStateOf<List<TradeItem>>(emptyList()) }

    // Read internal Python engine status and trade history.
    LaunchedEffect(Unit) {
        while (true) {
            try {
                val snapshot = withContext(Dispatchers.IO) {
                    engine.getBotStatus() to engine.getTrades(100)
                }

                val status = snapshot.first
                val running = status["running"] as? Boolean ?: false
                val message = status["text"]?.toString() ?: "غير شغال"
                val pythonConnected = status["connected"] as? Boolean ?: false
                val pythonMode = status["account_mode"]?.toString()?.uppercase()
                val verified = pythonConnected && (pythonMode == "PRACTICE" || pythonMode == "REAL")

                botMessage = message
                connected = verified
                accountMode = if (verified) pythonMode else null
                botState = when {
                    running -> BotState.RUNNING
                    message == "غير شغال" || message.contains("جاري") -> BotState.IDLE
                    else -> BotState.STOPPED
                }
                balance = if (verified) (status["balance"] as? Number)?.toDouble() else null
                dailyProfit = (status["pnl"] as? Number)?.toDouble() ?: 0.0

                trades = snapshot.second.map { item ->
                    TradeItem(
                        id = item["id"]?.toString() ?: "",
                        timestamp = (item["timestamp"] as? Number)?.toLong() ?: 0L,
                        pair = item["pair"]?.toString() ?: "-",
                        direction = item["direction"]?.toString() ?: "",
                        amount = (item["amount"] as? Number)?.toDouble() ?: 0.0,
                        result = item["result"]?.toString() ?: "PENDING",
                        profit = (item["profit"] as? Number)?.toDouble() ?: 0.0,
                        martingaleLevel = (item["martingaleLevel"] as? Number)?.toInt() ?: 1,
                        entryPrice = (item["entryPrice"] as? Number)?.toDouble() ?: 0.0,
                        exitPrice = (item["exitPrice"] as? Number)?.toDouble() ?: 0.0
                    )
                }
            } catch (e: Exception) {
                connected = false
            accountMode = null
            balance = null
            botMessage = "خطأ في قراءة حالة البوت: ${e.message ?: "غير معروف"}"
            }

            delay(2000)
        }
    }

    val layoutDirection =
        if (currentLanguage == "ar") LayoutDirection.Rtl
        else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection
    ) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = DarkSurface,
                    contentColor = TextPrimary,
                    tonalElevation = 0.dp
                ) {
                    AppTab.values().forEach { tab ->
                        val selected = currentTab == tab

                        NavigationBarItem(
                            selected = selected,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = stringResource(tab.labelRes),
                                    tint = if (selected) TealPrimary else TextMuted
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(tab.labelRes),
                                    color = if (selected) TealPrimary else TextMuted,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = TealDarkBg
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(DeepBlackBackground)
            ) {
                when (currentTab) {
                    AppTab.DASHBOARD -> DashboardScreen(
                        balance = balance,
                        connected = connected,
                        accountMode = accountMode,
                        dailyProfit = dailyProfit,
                        botState = botState,
                        botMessage = botMessage,
                        recentTrades = trades,
                        onStartBot = { mode, realConfirmed ->

                            val email = if (mode == "REAL") {
                                secureStorage.getRealAccountId()
                            } else {
                                secureStorage.getPracticeAccountId()
                            }

                            val password = if (mode == "REAL") {
                                secureStorage.getRealPassword()
                            } else {
                                secureStorage.getPracticePassword()
                            }

                            if (email.isBlank() || password.isNullOrBlank()) {
                                Toast.makeText(
                                    context,
                                    "من فضلك احفظ البريد الإلكتروني وكلمة المرور للحساب المختار من Settings",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                try {
                                    val intent = Intent(
                                        context,
                                        BotForegroundService::class.java
                                    ).apply {
                                        action = BotForegroundService.ACTION_START
                                        putExtra(BotForegroundService.EXTRA_EMAIL, email)
                                        putExtra(BotForegroundService.EXTRA_PASSWORD, password)
                                        putExtra(BotForegroundService.EXTRA_ACCOUNT_MODE, mode)
                                        putExtra(BotForegroundService.EXTRA_STRATEGY_MODE, selectedStrategyMode)
                                        putExtra(BotForegroundService.EXTRA_REAL_CONFIRMED, realConfirmed)
                                    }

                                    ContextCompat.startForegroundService(context, intent)

                                    botMessage = "جاري بدء التشغيل"
                                    botState = BotState.IDLE
                                    connected = false
                                    accountMode = null
                                    balance = null
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "تعذر تشغيل الخدمة: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        },
                        onStopBot = {
                            val intent = Intent(
                                context,
                                BotForegroundService::class.java
                            ).apply {
                                action = BotForegroundService.ACTION_STOP
                            }
                            context.startService(intent)
                            botMessage = "جارٍ طلب إيقاف البوت"
                        },
                        onNavigateToSettings = {
                            currentTab = AppTab.SETTINGS
                        }
                    )

                    AppTab.STRATEGY -> StrategyScreen(
                        selectedMode = selectedStrategyMode,
                        onModeChange = {
                            selectedStrategyMode = it
                        }
                    )

                    AppTab.HISTORY -> HistoryScreen(
                        trades = trades
                    )

                    AppTab.SETTINGS -> SettingsScreen(
                        practiceAccountId = secureStorage.getPracticeAccountId(),
                        hasPracticePassword = secureStorage.hasPracticePassword(),
                        realAccountId = secureStorage.getRealAccountId(),
                        hasRealPassword = secureStorage.hasRealPassword(),
                        currentLanguage = currentLanguage,
                        onSavePractice = { id, password ->
                            if (id.isNotEmpty()) {
                                secureStorage.savePracticeAccountId(id)
                            }
                            if (password.isNotEmpty()) {
                                secureStorage.savePracticePassword(password)
                            }
                        },
                        onSaveReal = { id, password ->
                            if (id.isNotEmpty()) {
                                secureStorage.saveRealAccountId(id)
                            }
                            if (password.isNotEmpty()) {
                                secureStorage.saveRealPassword(password)
                            }
                        },
                        onClearPractice = {
                            secureStorage.clearPracticeCredentials()
                        },
                        onClearReal = {
                            secureStorage.clearRealCredentials()
                        },
                        onChangeLanguage = { newLanguage ->
                            currentLanguage = newLanguage
                            secureStorage.saveLanguage(newLanguage)
                        }
                    )
                }
            }
        }
    }
}
