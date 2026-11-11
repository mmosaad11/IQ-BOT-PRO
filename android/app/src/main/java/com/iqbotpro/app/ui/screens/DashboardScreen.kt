package com.iqbotpro.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iqbotpro.app.R
import com.iqbotpro.app.data.model.BotState
import com.iqbotpro.app.data.model.ConnectionStatus
import com.iqbotpro.app.data.model.TradeItem
import com.iqbotpro.app.ui.theme.*
import java.util.Locale

@Composable
fun DashboardScreen(
    balance: Double?,
    dailyProfit: Double,
    connectionStatus: ConnectionStatus,
    botState: BotState,
    recentTrades: List<TradeItem>,
    onStartBot: () -> Unit,
    onStopBot: () -> Unit,
    onConnectBridge: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlackBackground)
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // App Header & Practice Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "IQ BOT PRO",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = TealPrimary,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = stringResource(R.string.strategy_desc),
                    style = MaterialTheme.typography.labelSmall
                )
            }

            // Practice Mode Locked Badge
            Surface(
                color = TealDarkBg,
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TealDeep)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Practice Mode",
                        tint = TealPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "PRACTICE ONLY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Balance & Daily Profit Cards
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.practice_balance),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = balance?.let { String.format(Locale.US, "$%,.2f", it) } ?: "في انتظار الرصيد",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = DarkSurfaceBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.daily_profit),
                            style = MaterialTheme.typography.labelSmall
                        )
                        val profitColor = when {
                            dailyProfit > 0 -> WinGreen
                            dailyProfit < 0 -> LossRed
                            else -> TextSecondary
                        }
                        val sign = if (dailyProfit > 0) "+" else ""
                        Text(
                            text = String.format(Locale.US, "%s$%,.2f", sign, dailyProfit),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                color = profitColor
                            )
                        )
                    }

                    // Bot State Indicator
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringResource(R.string.bot_status),
                            style = MaterialTheme.typography.labelSmall
                        )
                        val (stateBg, stateText) = when (botState) {
                            BotState.RUNNING -> Pair(TealDeep, WinGreen)
                            BotState.STOPPED -> Pair(Color(0xFF3B1D20), LossRed)
                            BotState.PAUSED -> Pair(Color(0xFF3A3114), DrawYellow)
                            BotState.IDLE -> Pair(DarkSurface, TextSecondary)
                        }
                        Surface(
                            color = stateBg,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = botState.name,
                                color = stateText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Connection Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusDotColor by animateColorAsState(
                        targetValue = when (connectionStatus) {
                            ConnectionStatus.CONNECTED -> WinGreen
                            ConnectionStatus.CONNECTING -> DrawYellow
                            ConnectionStatus.DISCONNECTED -> LossRed
                            ConnectionStatus.ERROR -> LossRed
                        },
                        label = "statusDotColor"
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusDotColor)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.connection_status),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = when (connectionStatus) {
                                ConnectionStatus.CONNECTED -> stringResource(R.string.status_connected)
                                ConnectionStatus.CONNECTING -> stringResource(R.string.status_connecting)
                                ConnectionStatus.DISCONNECTED -> stringResource(R.string.status_disconnected)
                                ConnectionStatus.ERROR -> "Connection Failed"
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                if (connectionStatus != ConnectionStatus.CONNECTED) {
                    OutlinedButton(
                        onClick = onConnectBridge,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TealPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TealDeep),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = stringResource(R.string.btn_connect), fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Main Controls: START and STOP Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onStartBot,
                enabled = botState != BotState.RUNNING,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TealPrimary,
                    contentColor = DeepBlackBackground,
                    disabledContainerColor = TealDeep.copy(alpha = 0.4f),
                    disabledContentColor = TextMuted
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Start", modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.btn_start),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Button(
                onClick = onStopBot,
                enabled = botState == BotState.RUNNING,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFC62828),
                    contentColor = Color.White,
                    disabledContainerColor = DarkSurfaceBorder,
                    disabledContentColor = TextMuted
                )
            ) {
                Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.btn_stop),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recent Trades Snippet
        Text(
            text = stringResource(R.string.trade_history),
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (recentTrades.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_trades),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        } else {
            recentTrades.take(3).forEach { trade ->
                TradeItemRow(trade)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun TradeItemRow(trade: TradeItem) {
    val resultColor = when (trade.result.uppercase()) {
        "WIN" -> WinGreen
        "LOSS" -> LossRed
        "DRAW" -> DrawYellow
        else -> TextSecondary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = trade.pair,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = if (trade.direction == "CALL") TealDeep else Color(0xFF421C1C),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = trade.direction,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (trade.direction == "CALL") TealPrimary else LossRed,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "M-Level ${trade.martingaleLevel} • $${trade.amount}",
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = resultColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = trade.result,
                        color = resultColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                val sign = if (trade.profit > 0) "+" else ""
                Text(
                    text = String.format(Locale.US, "%s$%,.2f", sign, trade.profit),
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = resultColor
                )
            }
        }
    }
}
