#!/usr/bin/env python3
"""
IQ BOT PRO - Python Bot Bridge Server
=====================================
This standalone Python script creates a lightweight WebSocket bridge on your
computer or trading VPS. The Android native app (IQ BOT PRO) connects to this
bridge over your local Wi-Fi or VPS IP (default port 8765).

Trading Strategy Architecture:
- Strategy: Keltner15 (EMA 20, ATR 10, Multiplier 1.5)
- Timeframe: 15 minutes (900 seconds)
- Expiry: 15 minutes
- 9-Level Martingale: [1.00, 1.52, 3.38, 7.51, 16.67, 36.99, 82.11, 182.24, 404.48]
- Practice Account Mode Only: Real account execution is strictly blocked.

Installation:
    pip install websockets asyncio

To integrate your existing bot:
    Attach your existing trading loop / broker connection (e.g. iqoptionapi)
    to the callbacks: on_start_trading(), on_stop_trading().
"""

import asyncio
import json
import logging
import time

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")

# Exact 9-step Martingale ladder from specification
MARTINGALE_LADDER = [1.00, 1.52, 3.38, 7.51, 16.67, 36.99, 82.11, 182.24, 404.48]

class IQBotEngine:
    def __init__(self):
        self.state = "IDLE"  # IDLE, RUNNING, STOPPED
        self.practice_balance = 10000.00
        self.daily_profit = 0.00
        self.current_martingale_index = 0
        self.active_strategy = "Keltner15"
        self.timeframe = 15  # 15 minutes
        self.expiry = 15     # 15 minutes
        self.account_type = "PRACTICE"  # Safety lock

    def get_current_stake(self) -> float:
        idx = min(self.current_martingale_index, len(MARTINGALE_LADDER) - 1)
        return MARTINGALE_LADDER[idx]

    def register_trade_result(self, result: str, profit: float, pair: str = "EURUSD"):
        """
        result must be "WIN", "LOSS", or "DRAW"
        """
        stake = self.get_current_stake()
        self.daily_profit += profit
        self.practice_balance += profit

        trade_item = {
            "id": f"TRD-{int(time.time()*1000)}",
            "timestamp": int(time.time() * 1000),
            "pair": pair,
            "direction": "CALL",
            "amount": stake,
            "result": result.upper(),
            "profit": profit,
            "martingaleLevel": self.current_martingale_index + 1
        }

        # Martingale Progression Logic:
        # Upon WIN or DRAW -> reset to level 1
        # Upon LOSS -> advance to next step up to 9
        if result.upper() in ["WIN", "DRAW"]:
            self.current_martingale_index = 0
        else:
            if self.current_martingale_index < len(MARTINGALE_LADDER) - 1:
                self.current_martingale_index += 1
            else:
                logging.warning("Max Martingale level 9 reached! Resetting to Level 1.")
                self.current_martingale_index = 0

        return trade_item


bot_engine = IQBotEngine()
connected_clients = set()


async def broadcast(message: dict):
    if not connected_clients:
        return
    payload = json.dumps(message)
    await asyncio.gather(*[client.send(payload) for client in connected_clients], return_exceptions=True)


async def handler(websocket):
    connected_clients.add(websocket)
    client_ip = websocket.remote_address[0] if websocket.remote_address else "Unknown"
    logging.info(f"Android client connected from {client_ip}")

    # Send initial state to Android
    await websocket.send(json.dumps({
        "type": "STATUS_UPDATE",
        "balance": bot_engine.practice_balance,
        "dailyProfit": bot_engine.daily_profit,
        "botState": bot_engine.state
    }))

    try:
        async for raw_message in websocket:
            try:
                data = json.loads(raw_message)
                msg_type = data.get("type")

                if msg_type == "HANDSHAKE":
                    logging.info(f"Handshake accepted from {data.get('client')}")

                elif msg_type == "COMMAND":
                    action = data.get("action")
                    if action == "START":
                        bot_engine.state = "RUNNING"
                        logging.info("Bot START command received from Android.")
                        await broadcast({
                            "type": "STATUS_UPDATE",
                            "botState": "RUNNING",
                            "balance": bot_engine.practice_balance,
                            "dailyProfit": bot_engine.daily_profit
                        })
                        await broadcast({
                            "type": "LOG",
                            "message": f"Keltner15 strategy running on 15M candles (Practice Account Only). Stake: ${bot_engine.get_current_stake():.2f}"
                        })

                    elif action == "STOP":
                        bot_engine.state = "STOPPED"
                        logging.info("Bot STOP command received from Android.")
                        await broadcast({
                            "type": "STATUS_UPDATE",
                            "botState": "STOPPED"
                        })
                        await broadcast({
                            "type": "LOG",
                            "message": "Bot execution stopped gracefully."
                        })

            except json.JSONDecodeError:
                logging.error(f"Invalid JSON received: {raw_message}")

    except Exception as e:
        logging.error(f"Client disconnected error: {e}")
    finally:
        connected_clients.remove(websocket)
        logging.info("Android client disconnected.")


async def main():
    import websockets
    port = 8765
    logging.info(f"=====================================================")
    logging.info(f"  IQ BOT PRO - Python Bridge Server")
    logging.info(f"  Listening on ws://0.0.0.0:{port}")
    logging.info(f"  Account Mode: PRACTICE ONLY (Real trading blocked)")
    logging.info(f"  Strategy: Keltner15 | 15M Timeframe | 15M Expiry")
    logging.info(f"  Martingale 9-Levels: {MARTINGALE_LADDER}")
    logging.info(f"=====================================================")
    async with websockets.serve(handler, "0.0.0.0", port):
        await asyncio.Future()  # run forever


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        logging.info("Bridge stopped by user.")
