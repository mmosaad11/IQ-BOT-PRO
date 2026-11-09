package com.iqbotpro.app.data.model

import kotlinx.serialization.Serializable

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

enum class BotState {
    IDLE,
    RUNNING,
    PAUSED,
    STOPPED
}

enum class TradeResult {
    WIN,
    LOSS,
    DRAW,
    PENDING
}

enum class TradeDirection {
    CALL,
    PUT
}

@Serializable
data class TradeItem(
    val id: String,
    val timestamp: Long,
    val pair: String,
    val direction: String, // "CALL" or "PUT"
    val amount: Double,
    val result: String, // "WIN", "LOSS", "DRAW", "PENDING"
    val profit: Double,
    val martingaleLevel: Int,
    val entryPrice: Double = 0.0,
    val exitPrice: Double = 0.0
)

data class Keltner15Config(
    val strategyName: String = "Keltner15",
    val timeframeMinutes: Int = 15,
    val expiryMinutes: Int = 15,
    val emaPeriod: Int = 20,
    val atrPeriod: Int = 10,
    val atrMultiplier: Double = 1.5,
    val accountType: String = "PRACTICE" // Strictly enforced practice account
)

object MartingaleConfig {
    // Exactly 9 levels specified in requirements:
    // 1.00, 1.52, 3.38, 7.51, 16.67, 36.99, 82.11, 182.24, 404.48
    val LEVELS = listOf(
        1.00,
        1.52,
        3.38,
        7.51,
        16.67,
        36.99,
        82.11,
        182.24,
        404.48
    )

    fun getAmountForLevel(levelIndex: Int): Double {
        val safeIndex = levelIndex.coerceIn(0, LEVELS.lastIndex)
        return LEVELS[safeIndex]
    }

    val totalRiskBudget: Double = LEVELS.sum()
}

@Serializable
data class BotBridgeMessage(
    val type: String, // "STATUS_UPDATE", "TRADE_EVENT", "COMMAND", "PING"
    val action: String? = null, // "START", "STOP", "SYNC"
    val balance: Double? = null,
    val dailyProfit: Double? = null,
    val botState: String? = null,
    val trade: TradeItem? = null,
    val message: String? = null
)
