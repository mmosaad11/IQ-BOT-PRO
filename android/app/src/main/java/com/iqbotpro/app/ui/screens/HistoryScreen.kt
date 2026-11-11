package com.iqbotpro.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iqbotpro.app.R
import com.iqbotpro.app.data.model.TradeItem
import com.iqbotpro.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(trades: List<TradeItem>) {
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredTrades = remember(trades, selectedFilter) {
        if (selectedFilter == "ALL") trades
        else trades.filter { it.result.equals(selectedFilter, ignoreCase = true) }
    }

    val totalWins = remember(trades) { trades.count { it.result.equals("WIN", ignoreCase = true) } }
    val totalLosses = remember(trades) { trades.count { it.result.equals("LOSS", ignoreCase = true) } }
    val totalDraws = remember(trades) { trades.count { it.result.equals("DRAW", ignoreCase = true) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlackBackground)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.trade_history),
            style = MaterialTheme.typography.headlineMedium.copy(color = TealPrimary)
        )
        Text(
            text = "Live trade stream from Python bot engine",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Stats Summary
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatPill("WIN", totalWins, WinGreen, modifier = Modifier.weight(1f))
            StatPill("LOSS", totalLosses, LossRed, modifier = Modifier.weight(1f))
            StatPill("DRAW", totalDraws, DrawYellow, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("ALL", "WIN", "LOSS", "DRAW").forEach { filterKey ->
                val isSelected = selectedFilter == filterKey
                Surface(
                    color = if (isSelected) TealPrimary else DarkSurfaceCard,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) TealPrimary else DarkSurfaceBorder
                    ),
                    modifier = Modifier.clickable { selectedFilter = filterKey }
                ) {
                    Text(
                        text = filterKey,
                        color = if (isSelected) DeepBlackBackground else TextSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredTrades.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
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
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredTrades, key = { it.id }) { trade ->
                    HistoryItemCard(trade)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun StatPill(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
            Text(
                text = "$count",
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun HistoryItemCard(trade: TradeItem) {
    val resultColor = when (trade.result.uppercase()) {
        "WIN" -> WinGreen
        "LOSS" -> LossRed
        "DRAW" -> DrawYellow
        else -> TextSecondary
    }

    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(trade.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = trade.pair,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
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
                    text = "$formattedTime • Martingale Level ${trade.martingaleLevel} • Stake $${trade.amount}",
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
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = resultColor
                )
            }
        }
    }
}
