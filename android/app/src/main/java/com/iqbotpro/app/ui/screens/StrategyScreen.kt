package com.iqbotpro.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iqbotpro.app.R
import com.iqbotpro.app.data.model.MartingaleConfig
import com.iqbotpro.app.ui.theme.*
import java.util.Locale

@Composable
fun StrategyScreen(
    selectedMode: String,
    onModeChange: (String) -> Unit
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

        Text(
            text = androidx.compose.ui.res.stringResource(R.string.strategy_title),
            style = MaterialTheme.typography.headlineMedium.copy(color = TealPrimary)
        )

        Text(
            text = androidx.compose.ui.res.stringResource(R.string.strategy_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "STRATEGY MODE",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        StrategyModeOption(
            title = "Both Strategies",
            description = "Run Keltner15 and Trend Reversal. Keltner is checked first.",
            mode = "both",
            selectedMode = selectedMode,
            onModeChange = onModeChange
        )

        Spacer(modifier = Modifier.height(8.dp))

        StrategyModeOption(
            title = "Keltner15 Only",
            description = "15-minute candles with 15-minute expiry.",
            mode = "keltner15",
            selectedMode = selectedMode,
            onModeChange = onModeChange
        )

        Spacer(modifier = Modifier.height(8.dp))

        StrategyModeOption(
            title = "Trend Reversal Only",
            description = "5-minute candles with 5-minute expiry.",
            mode = "trend_reversal",
            selectedMode = selectedMode,
            onModeChange = onModeChange
        )

        Spacer(modifier = Modifier.height(20.dp))

        StrategyInfoCard(
            title = "Keltner Channel Setup",
            timeframe = "15M",
            rows = listOf(
                "Timeframe" to "15 Minutes",
                "Expiry" to "15 Minutes",
                "Moving Average (EMA)" to "20 Periods",
                "Trend Filter (EMA)" to "400 Periods",
                "Volatility (ATR)" to "14 Periods",
                "Channel Multiplier" to "2.0x",
                "Account" to "Practice Only"
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        StrategyInfoCard(
            title = "Trend Reversal Setup",
            timeframe = "5M",
            rows = listOf(
                "Timeframe" to "5 Minutes",
                "Expiry" to "5 Minutes",
                "Minimum Streak" to "3 Consecutive Candles",
                "Streak Candle Body" to "At least 0.5 ATR(14)",
                "Reversal Candle" to "Opposite Direction, Non-Doji",
                "Entry Timing" to "After Reversal Candle Closes",
                "Entry Direction" to "Same as Reversal Candle"
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = androidx.compose.ui.res.stringResource(R.string.martingale_title),
            style = MaterialTheme.typography.titleMedium.copy(color = Color.White)
        )

        Text(
            text = androidx.compose.ui.res.stringResource(R.string.martingale_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("LEVEL", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text("STEP STAKE", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text("CUMULATIVE", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }

                HorizontalDivider(color = DarkSurfaceBorder)
                Spacer(modifier = Modifier.height(8.dp))

                var cumulative = 0.0

                MartingaleConfig.LEVELS.forEachIndexed { index, amount ->
                    cumulative += amount
                    val isFirst = index == 0

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = if (isFirst) TealDeep else DarkSurface,
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${index + 1}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFirst) TealPrimary else TextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = "Step ${index + 1}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isFirst) Color.White else TextSecondary
                            )
                        }

                        Text(
                            text = String.format(Locale.US, "$%.2f", amount),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = TealPrimary,
                            fontSize = 13.sp
                        )

                        Text(
                            text = String.format(Locale.US, "$%.2f", cumulative),
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = DarkSurfaceBorder)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total 9-Step Exposure",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = String.format(Locale.US, "$%.2f", cumulative),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = DrawYellow
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                TealDeep.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = TealPrimary,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "A WIN completes the cycle and resets to Level 1. " +
                        "A DRAW repeats the same amount, pair and direction. " +
                        "A LOSS advances to the next level. After 9 consecutive losses, the bot stops.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun StrategyModeOption(
    title: String,
    description: String,
    mode: String,
    selectedMode: String,
    onModeChange: (String) -> Unit
) {
    val selected = selectedMode == mode

    Card(
        onClick = { onModeChange(mode) },
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (selected) TealPrimary else DarkSurfaceBorder,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) TealDeep.copy(alpha = 0.35f) else DarkSurfaceCard
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = { onModeChange(mode) },
                colors = RadioButtonDefaults.colors(selectedColor = TealPrimary)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = description,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun StrategyInfoCard(
    title: String,
    timeframe: String,
    rows: List<Pair<String, String>>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Timeline,
                        contentDescription = null,
                        tint = TealPrimary
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }

                Surface(
                    color = TealDeep,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = timeframe,
                        color = TealPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = DarkSurfaceBorder)
            Spacer(modifier = Modifier.height(14.dp))

            rows.forEach { (label, value) ->
                ParamRow(label, value)
            }
        }
    }
}

@Composable
fun ParamRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}
