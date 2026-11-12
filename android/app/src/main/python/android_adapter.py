import threading
import bot

_engine_thread = None
_engine_lock = threading.Lock()


def start_bot(config):
    global _engine_thread

    with _engine_lock:
        if _engine_thread is not None and _engine_thread.is_alive():
            raise RuntimeError("البوت يعمل بالفعل")

        if not isinstance(config, dict):
            raise ValueError("إعدادات التشغيل غير صالحة")
        mode = str(config.get("account_mode", "PRACTICE")).upper()

        if mode not in ("PRACTICE", "REAL"):
            raise ValueError("وضع الحساب غير صالح")

        if mode == "REAL" and config.get("real_confirmed") is not True:
            raise RuntimeError("تم رفض تشغيل REAL: التأكيد الصريح غير موجود")

        if not config.get("email") or not config.get("password"):
            raise ValueError("بيانات الحساب غير مكتملة")

        config["account_mode"] = mode
        _engine_thread = threading.Thread(
            target=bot.run,
            args=(config,),
            name="IQBotEngine",
            daemon=True
        )
        _engine_thread.start()

    return {"started": True}


def stop_bot():
    bot.stop()
    return {"stop_requested": True}


def get_bot_status():
    return bot.get_status()


def get_trades(limit=100):
    import csv
    from datetime import datetime

    path = bot.TRADES_CSV
    if not __import__("os").path.exists(path):
        return []

    trades = []

    with open(path, "r", newline="", encoding="utf-8-sig") as f:
        reader = csv.DictReader(f)

        for index, row in enumerate(reader):
            try:
                trade_time = datetime.strptime(
                    row["time_cairo"], "%Y-%m-%d %H:%M:%S"
                ).replace(tzinfo=bot.TZ)

                timestamp = int(trade_time.timestamp() * 1000)

                trades.append({
                    "id": f"{timestamp}-{index}",
                    "timestamp": timestamp,
                    "pair": row["pair"],
                    "direction": row["direction"],
                    "amount": float(row["amount"]),
                    "result": row["result"].upper(),
                    "profit": float(row["profit"]),
                    "martingaleLevel": int(row["step"]),
                    "entryPrice": 0.0,
                    "exitPrice": 0.0
                })
            except (ValueError, KeyError, TypeError):
                continue

    return list(reversed(trades[-max(1, int(limit)):]))
