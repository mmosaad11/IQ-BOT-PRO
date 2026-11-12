package com.iqbotpro.app.data.network

import com.chaquo.python.PyObject
import com.chaquo.python.Python

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

    @Suppress("UNCHECKED_CAST")
    fun getBotStatus(): Map<String, Any?> {
        return adapter.callAttr("get_bot_status")
            .toJava(Map::class.java) as Map<String, Any?>
    }

    @Suppress("UNCHECKED_CAST")
    fun getTrades(limit: Int = 100): List<Map<String, Any?>> {
        return adapter.callAttr("get_trades", limit)
            .toJava(List::class.java) as List<Map<String, Any?>>
    }
}
