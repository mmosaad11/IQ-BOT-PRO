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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iqbotpro.app.R
import com.iqbotpro.app.data.model.MartingaleConfig
import com.iqbotpro.app.ui.theme.*
import java.util.Locale

@Composable
fun StrategyScreen() {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlackBackground)
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title
        Text(
            text = stringResource(R.string.strategy_title),
            style = MaterialTheme.typography.headlineMedium.copy(color = TealPrimary)
        )
        Text(
            text = stringResource(R.string.strategy_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Strategy Parameters Card
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
                        Icon(Icons.Default.Timeline, contentDescription = null, tint = TealPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Keltner Channel Setup",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                    Surface(
                        color = TealDeep,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "15M",
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

                ParamRow(stringResource(R.string.timeframe_label), stringResource(R.string.timeframe_val))
                ParamRow(stringResource(R.string.expiry_label), stringResource(R.string.expiry_val))
                ParamRow("Moving Average (EMA)", "20 Periods")
                ParamRow("Volatility Band (ATR)", "10 Periods")
                ParamRow("Channel Multiplier", "1.50x")
                ParamRow("Account Security Mode", "Practice Account Only")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 9-Level Martingale Ladder
        Text(
            text = stringResource(R.string.martingale_title),
            style = MaterialTheme.typography.titleMedium.copy(color = Color.White)
        )
        Text(
            text = stringResource(R.string.martingale_desc),
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
                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "LEVEL", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(text = "STEP STAKE", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(text = "CUMULATIVE", style = MaterialTheme.typography.labelSmall, color = TextMuted)
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
                    Text(text = "Total 9-Step Exposure", fontWeight = FontWeight.Bold, color = Color.White)
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

        // Risk Note
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, TealDeep.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "The 9-step progression resets to Level 1 ($1.00) immediately upon any WIN or DRAW event.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
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
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}
