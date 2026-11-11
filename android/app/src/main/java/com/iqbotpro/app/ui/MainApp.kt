package com.iqbotpro.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.iqbotpro.app.R
import com.iqbotpro.app.data.network.BotBridgeClient
import com.iqbotpro.app.data.security.SecureStorageManager
import com.iqbotpro.app.ui.screens.*
import com.iqbotpro.app.ui.theme.*

enum class AppTab(val icon: ImageVector, val labelRes: Int) {
    DASHBOARD(Icons.Default.Dashboard, R.string.tab_dashboard),
    STRATEGY(Icons.Default.Equalizer, R.string.tab_strategy),
    HISTORY(Icons.Default.History, R.string.tab_history),
    SETTINGS(Icons.Default.Settings, R.string.tab_settings)
}

@Composable
fun MainApp(
    bridgeClient: BotBridgeClient,
    secureStorage: SecureStorageManager
) {
    var currentTab by remember { mutableStateOf(AppTab.DASHBOARD) }
    var currentLanguage by remember { mutableStateOf(secureStorage.getLanguage()) }
    var selectedStrategyMode by remember { mutableStateOf("both") }

    val connectionStatus by bridgeClient.connectionStatus.collectAsState()
    val botState by bridgeClient.botState.collectAsState()
    val balance by bridgeClient.practiceBalance.collectAsState()
    val dailyProfit by bridgeClient.dailyProfit.collectAsState()
    val trades by bridgeClient.trades.collectAsState()

    LaunchedEffect(Unit) {
        bridgeClient.connect(secureStorage.getBridgeHost())
    }

    val layoutDirection = if (currentLanguage == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
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
                        dailyProfit = dailyProfit,
                        connectionStatus = connectionStatus,
                        botState = botState,
                        recentTrades = trades,
                        onStartBot = { bridgeClient.startBot(selectedStrategyMode) },
                        onStopBot = { bridgeClient.stopBot() },
                        onConnectBridge = { bridgeClient.connect(secureStorage.getBridgeHost()) },
                        onNavigateToSettings = { currentTab = AppTab.SETTINGS }
                    )
                    AppTab.STRATEGY -> StrategyScreen(
                        selectedMode = selectedStrategyMode,
                        onModeChange = { selectedStrategyMode = it }
                    )
                    AppTab.HISTORY -> HistoryScreen(trades = trades)
                    AppTab.SETTINGS -> SettingsScreen(
                        currentHost = secureStorage.getBridgeHost(),
                        currentAccountId = secureStorage.getPracticeAccountId(),
                        hasSavedPassword = secureStorage.hasSavedPassword(),
                        currentLanguage = currentLanguage,
                        onSaveSettings = { host, id, pwd ->
                            secureStorage.saveBridgeHost(host)
                            if (id.isNotEmpty()) secureStorage.savePracticeAccountId(id)
                            if (pwd.isNotEmpty()) secureStorage.savePracticePassword(pwd)
                        },
                        onClearCredentials = {
                            secureStorage.clearCredentials()
                        },
                        onChangeLanguage = { newLang ->
                            currentLanguage = newLang
                            secureStorage.saveLanguage(newLang)
                        }
                    )
                }
            }
        }
    }
}
