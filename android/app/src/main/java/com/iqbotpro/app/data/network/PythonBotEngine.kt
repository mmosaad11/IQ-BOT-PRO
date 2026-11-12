package com.iqbotpro.app.data.network

import com.chaquo.python.PyObject
import com.chaquo.python.Python
import org.json.JSONArray
import org.json.JSONObject

class PythonBotEngine {
    private val adapter: PyObject by lazy {
        Python.getInstance().getModule("android_adapter")
    }

    fun startBot(config: Map<String, Any>) {
        adapter.callAttr("start_bot", HashMap(config))
    }

    fun stopBot() {
        adapter.callAttr("stop_bot")
    }

    fun getBotStatus(): Map<String, Any?> {
        val json = JSONObject(adapter.callAttr("get_bot_status").toString())
        return mapOf(
            "running" to json.optBoolean("running", false),
            "text" to json.optString("text", "غير شغال"),
            "balance" to if (json.isNull("balance")) null else json.optDouble("balance"),
            "pnl" to json.optDouble("pnl", 0.0),
            "step" to json.optInt("step", 1),
            "amount" to json.optDouble("amount", 0.0),
            "pair" to json.optString("pair", "-")
        )
    }

    fun getTrades(limit: Int = 100): List<Map<String, Any?>> {
        val json = JSONArray(adapter.callAttr("get_trades", limit).toString())
        return (0 until json.length()).map { index ->
            val item = json.getJSONObject(index)
            mapOf(
                "id" to item.optString("id", ""),
                "timestamp" to item.optLong("timestamp", 0L),
                "pair" to item.optString("pair", "-"),
                "direction" to item.optString("direction", ""),
                "amount" to item.optDouble("amount", 0.0),
                "result" to item.optString("result", "PENDING"),
                "profit" to item.optDouble("profit", 0.0),
                "martingaleLevel" to item.optInt("martingaleLevel", 1),
                "entryPrice" to item.optDouble("entryPrice", 0.0),
                "exitPrice" to item.optDouble("exitPrice", 0.0)
            )
        }
    }
}
