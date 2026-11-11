#!/usr/bin/env python3
import asyncio
import csv
import importlib
import json
import logging
import os
import queue
import sys
import threading
import time

import websockets

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s"
)

BOT_DIR = os.path.expanduser("~/iqbot_phone")
PORT = 8765

sys.path.insert(0, BOT_DIR)

bot = importlib.import_module("bot")

clients = set()
events = queue.Queue()
bot_thread = None
thread_lock = threading.Lock()


# Relay original bot logs without changing its logging behavior.
_original_log = bot.log

def bridged_log(message, *args, **kwargs):
    _original_log(message, *args, **kwargs)
    events.put({
        "type": "LOG",
        "message": str(message)
    })

bot.log = bridged_log


# Relay real trades after the original CSV writer saves them.
_original_log_trade = bot.log_trade

def bridged_log_trade(row):
    _original_log_trade(row)

    try:
        trade = {
            "id": f"TRD-{time.time_ns()}",
            "timestamp": int(time.time() * 1000),
            "pair": str(row[1]),
            "direction": str(row[2]).upper(),
            "amount": float(row[4]),
            "result": str(row[7]).upper(),
            "profit": float(row[8]),
            "martingaleLevel": int(row[3]),
        }
        events.put({
            "type": "TRADE_EVENT",
            "trade": trade
        })
    except Exception as exc:
        events.put({
            "type": "LOG",
            "message": f"Trade display error: {exc}"
        })

bot.log_trade = bridged_log_trade


def current_state():
    if bot_thread is None:
        return "IDLE"
    if bot_thread.is_alive():
        return "RUNNING"
    return "STOPPED"


def status_message():
    status = bot.STATUS
    return {
        "type": "STATUS_UPDATE",
        "balance": status.get("balance"),
        "dailyProfit": float(status.get("pnl", 0.0)),
        "botState": current_state()
    }


async def broadcast(message):
    if not clients:
        return

    payload = json.dumps(message, ensure_ascii=False)
    disconnected = []

    for client in list(clients):
        try:
            await client.send(payload)
        except Exception:
            disconnected.append(client)

    for client in disconnected:
        clients.discard(client)


def start_original_bot(strategy_mode="keltner15"):
    global bot_thread

    allowed_modes = {"both", "keltner15", "trend_reversal"}
    if strategy_mode not in allowed_modes:
        strategy_mode = "keltner15"

    with thread_lock:
        if bot_thread is not None and bot_thread.is_alive():
            return False, "البوت يعمل بالفعل."

        cfg = bot.load_cfg()

        if not cfg.get("email") or not cfg.get("password"):
            return False, "بيانات الدخول غير موجودة في إعدادات البوت."

        cfg["strategy_mode"] = strategy_mode

        bot_thread = threading.Thread(
            target=bot.run,
            args=(cfg,),
            name="Original-IQ-Bot",
            daemon=True
        )
        bot_thread.start()

    return True, f"تم تشغيل البوت بوضع الاستراتيجيات: {strategy_mode}."


def stop_original_bot():
    if bot_thread is None or not bot_thread.is_alive():
        return False

    bot.STOP.set()
    return True


async def handler(websocket):
    clients.add(websocket)
    logging.info("Android app connected.")

    try:
        await websocket.send(json.dumps(
            status_message(),
            ensure_ascii=False
        ))

        async for raw_message in websocket:
            try:
                data = json.loads(raw_message)
            except json.JSONDecodeError:
                continue

            msg_type = data.get("type")

            if msg_type == "HANDSHAKE":
                await websocket.send(json.dumps({
                    "type": "LOG",
                    "message": "Connected to original Python bot bridge."
                }))

            elif msg_type == "COMMAND":
                action = str(data.get("action", "")).upper()

                if action == "START":
                    ok, message = start_original_bot(str(data.get("strategyMode", "keltner15")))
                    await broadcast({
                        "type": "LOG",
                        "message": message
                    })
                    await broadcast(status_message())

                elif action == "STOP":
                    stopped = stop_original_bot()
                    message = (
                        "تم إرسال طلب إيقاف البوت."
                        if stopped else "البوت غير مشغّل حاليًا."
                    )
                    await broadcast({
                        "type": "LOG",
                        "message": message
                    })

    except websockets.exceptions.ConnectionClosed:
        pass
    except Exception as exc:
        logging.exception("Client handler error: %s", exc)
    finally:
        clients.discard(websocket)
        logging.info("Android app disconnected.")


async def monitor_bot():
    while True:
        try:
            while True:
                event = events.get_nowait()
                await broadcast(event)
        except queue.Empty:
            pass

        await broadcast(status_message())
        await asyncio.sleep(1)


async def main():
    logging.info("IQ BOT PRO - Original Bot Bridge")
    logging.info("Listening on ws://127.0.0.1:%s", PORT)
    logging.info("Trading logic remains in the original bot.py")
    logging.info("Practice account protection remains enabled.")

    asyncio.create_task(monitor_bot())

    async with websockets.serve(
        handler,
        "127.0.0.1",
        PORT
    ):
        await asyncio.Future()


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        logging.info("Bridge stopped.")
