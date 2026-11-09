package com.iqbotpro.app.data.network

import com.iqbotpro.app.data.model.BotBridgeMessage
import com.iqbotpro.app.data.model.BotState
import com.iqbotpro.app.data.model.ConnectionStatus
import com.iqbotpro.app.data.model.TradeItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

/**
 * Decoupled WebSocket Client for the external Python bot engine.
 *
 * Adheres strictly to the architectural boundary:
 * - The Android app acts as a remote control and monitoring UI.
 * - Trading execution, Keltner15 indicators, and broker connectivity reside in the Python script.
 * - Connection status truthfully reflects physical socket connectivity.
 */
class BotBridgeClient(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .readTimeout(10, TimeUnit.SECONDS)
        .connectTimeout(5, TimeUnit.SECONDS)
        .build()

    private var activeWebSocket: WebSocket? = null

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _botState = MutableStateFlow(BotState.IDLE)
    val botState: StateFlow<BotState> = _botState.asStateFlow()

    private val _practiceBalance = MutableStateFlow(10000.00)
    val practiceBalance: StateFlow<Double> = _practiceBalance.asStateFlow()

    private val _dailyProfit = MutableStateFlow(0.00)
    val dailyProfit: StateFlow<Double> = _dailyProfit.asStateFlow()

    private val _trades = MutableStateFlow<List<TradeItem>>(emptyList())
    val trades: StateFlow<List<TradeItem>> = _trades.asStateFlow()

    private val _statusLogs = MutableSharedFlow<String>()
    val statusLogs: SharedFlow<String> = _statusLogs.asSharedFlow()

    fun connect(bridgeUrl: String) {
        if (_connectionStatus.value == ConnectionStatus.CONNECTING ||
            _connectionStatus.value == ConnectionStatus.CONNECTED) {
            return
        }

        _connectionStatus.value = ConnectionStatus.CONNECTING
        val request = try {
            Request.Builder().url(bridgeUrl).build()
        } catch (e: Exception) {
            _connectionStatus.value = ConnectionStatus.ERROR
            scope.launch { _statusLogs.emit("Invalid bridge URL: ${e.message}") }
            return
        }

        activeWebSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionStatus.value = ConnectionStatus.CONNECTED
                scope.launch {
                    _statusLogs.emit("Connected to Python bot bridge at $bridgeUrl")
                    // Send initial handshake
                    webSocket.send("""{"type":"HANDSHAKE","client":"IQ_BOT_PRO_ANDROID","accountMode":"PRACTICE"}""")
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                scope.launch {
                    try {
                        val msg = json.decodeFromString<BotBridgeMessage>(text)
                        when (msg.type) {
                            "STATUS_UPDATE" -> {
                                msg.balance?.let { _practiceBalance.value = it }
                                msg.dailyProfit?.let { _dailyProfit.value = it }
                                msg.botState?.let {
                                    _botState.value = try {
                                        BotState.valueOf(it.uppercase())
                                    } catch (_: Exception) {
                                        BotState.IDLE
                                    }
                                }
                            }
                            "TRADE_EVENT" -> {
                                msg.trade?.let { newTrade ->
                                    val currentList = _trades.value.toMutableList()
                                    currentList.add(0, newTrade)
                                    _trades.value = currentList
                                    _dailyProfit.value += newTrade.profit
                                }
                            }
                            "LOG" -> {
                                msg.message?.let { _statusLogs.emit(it) }
                            }
                        }
                    } catch (e: Exception) {
                        _statusLogs.emit("Protocol message parse error: ${e.message}")
                    }
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
                scope.launch {
                    _statusLogs.emit("Connection lost: ${t.localizedMessage ?: "Offline"}")
                }
            }
        })
    }

    fun disconnect() {
        activeWebSocket?.close(1000, "User disconnected")
        activeWebSocket = null
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
        _botState.value = BotState.IDLE
    }

    fun startBot() {
        if (_connectionStatus.value != ConnectionStatus.CONNECTED) {
            scope.launch {
                _statusLogs.emit("Cannot start: Python bot bridge is not connected.")
            }
            return
        }
        val command = """{"type":"COMMAND","action":"START","strategy":"Keltner15","timeframe":15,"expiry":15,"accountType":"PRACTICE"}"""
        activeWebSocket?.send(command)
        _botState.value = BotState.RUNNING
    }

    fun stopBot() {
        if (_connectionStatus.value != ConnectionStatus.CONNECTED) {
            _botState.value = BotState.STOPPED
            return
        }
        val command = """{"type":"COMMAND","action":"STOP"}"""
        activeWebSocket?.send(command)
        _botState.value = BotState.STOPPED
    }
}
