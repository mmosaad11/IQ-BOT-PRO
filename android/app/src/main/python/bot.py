# -*- coding: utf-8 -*-
"""
بوت IQ Option - حساب Practice فقط

استراتيجيتين (الإعداد "strategy" في config.json):
  1) keltner15 (الافتراضية): فريم 15 دقيقة، انتهاء 15 دقيقة.
       سعر الإغلاق خارج قناة Keltner (EMA20 ± 2×ATR14) -> دخول عكس الاختراق،
       بشرط: الصفقة مع اتجاه EMA400، والتقلب هادي (ATR الحالي < متوسط آخر 200 شمعة).
  2) streak: 3 شموع أو أكثر متتالية بنفس الاتجاه ثم شمعة عكسية -> دخول مع الشمعة العكسية (5 دقايق).
مضاعفات حسب الجدول. بعد أي خسارة: المضاعفة التالية على نفس الزوج وبنفس الاتجاه لحد ما صفقة تكسب.
"""
import csv
import json
import logging
import os
import sys
import threading
import time
from datetime import datetime, timedelta
from zoneinfo import ZoneInfo

from iqoptionapi.stable_api import IQ_Option

# لو البرنامج اتحوّل لـ exe، الملفات (الإعدادات والسجلات) بتتحفظ جنب الـ exe
BASE_DIR = os.environ.get("IQBOT_DATA_DIR") or (
    os.path.dirname(sys.executable)
    if getattr(sys, "frozen", False)
    else os.path.dirname(os.path.abspath(__file__))
)
os.makedirs(BASE_DIR, exist_ok=True)

CFG_PATH = os.path.join(BASE_DIR, "config.json")
STATE_PATH = os.path.join(BASE_DIR, "state.json")
TRADES_CSV = os.path.join(BASE_DIR, "trades.csv")
TZ = ZoneInfo("Africa/Cairo")
CANDLE = 900      # فريم الشمعة بالثواني (بيتظبط من الإعدادات في configure)
EXPIRY_MIN = 15   # مدة الصفقة بالدقايق
INSTRUMENT = "binary"   # نوع العقد في IQ: turbo (حتى 5 دقايق) / binary (15 دقيقة)

FX_CCY = {"USD", "EUR", "GBP", "AUD", "NZD", "CAD", "CHF", "JPY"}
VALIDATED_PAIRS = ["EURUSD", "GBPUSD", "AUDUSD", "USDCAD"]   # الأزواج اللي اتختبرت فعلًا

_handlers = [logging.FileHandler(os.path.join(BASE_DIR, "bot.log"), encoding="utf-8")]
if sys.stdout is not None:
    _handlers.append(logging.StreamHandler(sys.stdout))
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s | %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
    handlers=_handlers,
)
log = logging.info

DEFAULT_CFG = {
    "email": "",
    "password": "",
    "start_hour": 8,
    "end_hour": 22,
    "finish_cycle_after_hours": True,
    "daily_profit_target": 5.0,
    "min_payout": 0.82,
    "strategy": "keltner15",
    "pairs_mode": "all_fx",        # all_fx = كل أزواج الفوركس المتاحة | validated = الأربعة المختبرين | list = قائمة "pairs"
    "pairs": [],
    "history_candles": 1000,
    "min_streak": 3,
    "include_otc": False,
    "max_entry_delay_sec": 45,
    "max_wait_candles_same_pair": 12,
    "martingale": [1.00, 1.52, 3.38, 7.51, 16.67, 36.99, 82.11, 182.24, 404.48],
}

# ---------------------------- التحكم من الواجهة ----------------------------
STOP = threading.Event()
STATUS = {"running": False, "text": "غير شغال", "pnl": 0.0, "step": 1, "amount": 0.0,
          "pair": "-", "balance": None}


class StopBot(Exception):
    pass


def nap(sec):
    """نوم يتقطع فورًا لو اتضغط إيقاف."""
    if STOP.wait(max(0, sec)):
        raise StopBot()


def set_status(**kw):
    STATUS.update(kw)
def stop():
    STOP.set()
def get_status():
    return dict(STATUS)
def load_cfg():
    cfg = dict(DEFAULT_CFG)
    if os.path.exists(CFG_PATH):
        with open(CFG_PATH, "r", encoding="utf-8") as f:
            cfg.update(json.load(f))
    return cfg


def save_cfg(cfg):
    with open(CFG_PATH, "w", encoding="utf-8") as f:
        json.dump(cfg, f, ensure_ascii=False, indent=2)


def cairo_now():
    return datetime.now(TZ)


# ---------------------------- الحالة (تفضل محفوظة لو قفلت البوت) ----------------------------
def load_state():
    today = cairo_now().strftime("%Y-%m-%d")
    st = new_day_state(today)

    if os.path.exists(STATE_PATH):
        try:
            with open(STATE_PATH, "r", encoding="utf-8") as f:
                old = json.load(f)

            if old.get("date") == today:
                st.update(old)
            else:
                # دورة المارتينجال المفتوحة تستمر حتى بعد بداية يوم جديد.
                st["step"] = old.get("step", 0)
                st["pair"] = old.get("pair")
                st["direction"] = old.get("direction")
                st["cycle_strategy"] = old.get("cycle_strategy")

        except Exception:
            pass

    return st

def new_day_state(today, prev=None):
    st = {
        "date": today,
        "pnl": 0.0,
        "step": 0,
        "pair": None,
        "direction": None,
        "cycle_strategy": None,
        "wait": 0,
        "stopped": False,
        "stop_reason": "",
    }

    if prev:
        st["step"] = prev.get("step", 0)
        st["pair"] = prev.get("pair")
        st["direction"] = prev.get("direction")
        st["cycle_strategy"] = prev.get("cycle_strategy")

    return st

def save_state(st):
    with open(STATE_PATH, "w", encoding="utf-8") as f:
        json.dump(st, f, ensure_ascii=False, indent=2)


def log_trade(row):
    new = not os.path.exists(TRADES_CSV)
    with open(TRADES_CSV, "a", newline="", encoding="utf-8-sig") as f:
        w = csv.writer(f)
        if new:
            w.writerow(["time_cairo", "pair", "direction", "step", "amount",
                        "payout", "streak", "result", "profit", "day_pnl"])
        w.writerow(row)


def refresh(st, amounts, text=None):
    step = min(st["step"], len(amounts) - 1)
    set_status(pnl=st["pnl"], step=step + 1, amount=amounts[step],
               pair=st.get("pair") or "-")
    if text:
        set_status(text=text)


# ---------------------------- الاتصال ----------------------------
def selected_account_mode(cfg):
    mode = str(cfg.get("account_mode", "PRACTICE")).upper()
    if mode not in ("PRACTICE", "REAL"):
        raise RuntimeError("وضع الحساب غير صالح")
    return mode


def apply_account_mode(api, cfg):
    mode = selected_account_mode(cfg)

    profile = api.get_profile_ansyc()
    balances = profile.get("balances") or []

    expected_type = 1 if mode == "REAL" else 4
    account = next(
        (b for b in balances if b.get("type") == expected_type),
        None
    )

    if account is None or not account.get("id"):
        raise RuntimeError(f"حساب {mode} غير موجود في المنصة")

    api.change_balance(mode)

    actual_mode = str(api.get_balance_mode()).upper()
    if actual_mode != mode:
        raise RuntimeError(
            f"فشل التحقق من الحساب: المطلوب {mode} والمحدد {actual_mode}"
        )

    log(f"تم التحقق من حساب {mode}")
    return mode


def connect(cfg):
    api = IQ_Option(cfg["email"], cfg["password"])
    ok, reason = api.connect()

    if not ok:
        raise RuntimeError(f"فشل تسجيل الدخول: {reason}")

    mode = apply_account_mode(api, cfg)
    log(f"تم الاتصال - حساب {mode} - الرصيد: {api.get_balance()}")
    return api


def ensure_connected(api, cfg):
    try:
        if api.check_connect():
            apply_account_mode(api, cfg)
            return api
    except Exception:
        pass

    log("انقطع الاتصال، إعادة محاولة...")

    for _ in range(5):
        try:
            ok, _r = api.connect()
            if ok:
                apply_account_mode(api, cfg)
                log("تم إعادة الاتصال والتحقق من الحساب")
                return api
        except Exception as e:
            log(f"فشلت محاولة إعادة الاتصال: {e}")

        nap(5)

    log("تعذر إعادة الاتصال حاليًا")
    return None


# ---------------------------- الإشارة ----------------------------
def color(c):
    if c["close"] > c["open"]:
        return 1    # صاعدة
    if c["close"] < c["open"]:
        return -1   # هابطة
    return 0        # دوجي


def check_pattern(candles, min_streak):
    """كشف انعكاس بعد سلسلة شموع متتالية متوسطة أو كبيرة."""
    if len(candles) < min_streak + 1:
        return None, 0

    atr = _atr_series(candles)
    rev = color(candles[-1])
    if rev == 0:
        return None, 0

    streak = 0
    for i in range(len(candles) - 2, -1, -1):
        c = candles[i]
        body = abs(c["close"] - c["open"])
        if color(c) != -rev or atr[i] <= 0 or body < 0.5 * atr[i]:
            break
        streak += 1

    if streak >= min_streak:
        return ("call" if rev == 1 else "put"), streak

    return None, 0

def configure_strategy(strategy):
    """يضبط الفريم والانتهاء ونوع العقد للاستراتيجية المحددة."""
    global CANDLE, EXPIRY_MIN, INSTRUMENT
    if strategy == "keltner15":
        CANDLE, EXPIRY_MIN, INSTRUMENT = 900, 15, "binary"
    elif strategy == "trend_reversal":
        CANDLE, EXPIRY_MIN, INSTRUMENT = 300, 5, "turbo"
    else:
        raise ValueError(f"استراتيجية غير معروفة: {strategy}")


def configure(cfg):
    """توافق مع إعداد الاستراتيجية القديمة."""
    strategy = cfg.get("strategy", "keltner15")
    configure_strategy(
        "keltner15" if strategy == "keltner15" else "trend_reversal"
    )

def _ema_last(vals, p):
    k = 2.0 / (p + 1)
    e = vals[0]
    for x in vals[1:]:
        e = e + k * (x - e)
    return e


def _atr_series(cs):
    """ATR14 بطريقة Wilder (نفس الباك تست): أول قيمة = أول TR."""
    out, a, prev = [], None, None
    for x in cs:
        tr = x["max"] - x["min"] if prev is None else max(
            x["max"] - x["min"], abs(x["max"] - prev), abs(x["min"] - prev))
        a = tr if a is None else (a * 13 + tr) / 14
        out.append(a)
        prev = x["close"]
    return out


def keltner_signal(closed, mult=2.0, trend_ema=400, vol_filter=True):
    """closed: شموع مقفولة (مفاتيح IQ: open/close/max/min/from) مرتبة تصاعديًا.
    بيرجّع ("call"/"put", تفاصيل) أو (None, سبب)."""
    if len(closed) < trend_ema + 50:
        return None, "شموع قليلة"
    cl = [x["close"] for x in closed]
    at = _atr_series(closed)
    e20 = _ema_last(cl, 20)
    e_tr = _ema_last(cl, trend_ema)
    last, a = cl[-1], at[-1]
    if last < e20 - mult * a:
        side = 1
    elif last > e20 + mult * a:
        side = -1
    else:
        return None, "جوه القناة"
    if (side == 1) != (last > e_tr):
        return None, "عكس اتجاه EMA400"
    rel = a / (sum(at[-200:]) / 200)
    if vol_filter and not rel < 1.0:
        return None, f"تقلب عالي ({rel:.2f})"
    return ("call" if side == 1 else "put"), f"rel={rel:.2f}"


def fx_pair_ok(name, cfg):
    """هل الاسم ده زوج فوركس مسموح بيه حسب إعدادات pairs_mode؟"""
    if name.endswith("-OTC") and not cfg.get("include_otc", False):
        return False
    mode = cfg.get("pairs_mode", "all_fx")
    if mode == "validated":
        return name in VALIDATED_PAIRS
    if mode == "list":
        return name in cfg.get("pairs", [])
    base = name[:-4] if name.endswith("-OTC") else name
    return len(base) == 6 and base[:3] in FX_CCY and base[3:] in FX_CCY


_HIST = {}   # كاش الشموع المقفولة لكل زوج (عشان منحمّلش 1000 شمعة كل مرة)


def get_closed_candles(api, name, cfg, candle_seconds=None):
    candle_seconds = candle_seconds or CANDLE
    now_ts = time.time()
    cache_key = (name, candle_seconds)
    hist = _HIST.get(cache_key)
    n = cfg.get("history_candles", 1000)

    if hist is None or not hist or now_ts - (hist[-1]["from"] + candle_seconds) > 4 * candle_seconds:
        cs = api.get_candles(name, candle_seconds, n, now_ts)
    else:
        cs = api.get_candles(name, candle_seconds, 6, now_ts)

    merged = {c["from"]: c for c in (hist or [])}
    for c in cs:
        if c["from"] + candle_seconds <= now_ts:
            merged[c["from"]] = c

    out = [merged[k] for k in sorted(merged)][-max(n, 500):]
    _HIST[cache_key] = out
    return out


def find_signal(api, cfg, strategy=None):
    strategy = strategy or cfg.get("strategy", "keltner15")
    configure_strategy(strategy)

    min_payout = cfg["min_payout"]
    candle_seconds = 900 if strategy == "keltner15" else 300

    try:
        opens = api.get_all_open_time().get(INSTRUMENT, {})
        profits = api.get_all_profit()
    except Exception as e:
        log(f"تعذر جلب الأزواج: {e}")
        return None

    pairs = []
    for name, v in opens.items():
        if not v.get("open"):
            continue

        if strategy == "keltner15":
            if not fx_pair_ok(name, cfg):
                continue
        elif not cfg.get("include_otc", True) and name.endswith("-OTC"):
            continue

        payout = (profits.get(name) or {}).get(INSTRUMENT)
        if payout is None or payout < min_payout:
            continue
        pairs.append((name, payout))

    pairs.sort(key=lambda x: x[1], reverse=True)
    deadline = time.time() + cfg["max_entry_delay_sec"]

    for name, payout in pairs:
        if STOP.is_set():
            raise StopBot()
        if time.time() > deadline:
            log("انتهت مهلة الفحص - هنستنى الشمعة الجاية")
            return None

        try:
            closed = get_closed_candles(
                api, name, cfg, candle_seconds=candle_seconds
            )
            if not closed:
                continue

            if strategy == "keltner15":
                if closed[-1]["from"] + candle_seconds < time.time() - candle_seconds:
                    continue
                direction, info = keltner_signal(closed, 2.0, 400, True)
                streak = "keltner"
            else:
                direction, streak = check_pattern(
                    closed, cfg["min_streak"]
                )
                info = f"انعكاس بعد {streak} شموع"

            if direction:
                log(f"إشارة {strategy} على {name}: {direction} ({info})")
                return {
                    "pair": name,
                    "direction": direction,
                    "streak": streak,
                    "payout": payout,
                    "strategy": strategy,
                }

        except Exception as e:
            log(f"خطأ في فحص {name}: {e}")

    return None


def pair_payout_if_ready(api, cfg, pair, strategy=None):
    """يفحص فتح الزوج ونسبة العائد وفق نوع عقد الاستراتيجية."""
    if strategy:
        configure_strategy(strategy)

    try:
        opens = api.get_all_open_time().get(INSTRUMENT, {})
        if not (opens.get(pair) or {}).get("open"):
            return None

        payout = (api.get_all_profit().get(pair) or {}).get(INSTRUMENT)
        if payout is None or payout < cfg["min_payout"]:
            return None
        return payout
    except Exception as e:
        log(f"تعذر فحص الزوج {pair}: {e}")
        return None


def place_and_wait(api, sig, amount, cfg):
    expected_mode = selected_account_mode(cfg)
    actual_mode = str(api.get_balance_mode()).upper()
    if actual_mode != expected_mode:
        raise RuntimeError(
            f"إيقاف التداول: الحساب الفعلي {actual_mode} والمطلوب {expected_mode}"
        )
    ok, order_id = api.buy(amount, sig["pair"], sig["direction"], EXPIRY_MIN)
    if not ok:
        log(f"فشل فتح الصفقة: {order_id}")
        return None
    log(f"تم فتح صفقة {sig['pair']} {sig['direction']} بمبلغ {amount}$ (id={order_id})")
    set_status(text=f"صفقة مفتوحة على {sig['pair']} ({amount}$) - بانتظار النتيجة")
    res = api.check_win_v3(order_id)   # بتستنى لحد نهاية الصفقة
    if isinstance(res, (tuple, list)):
        res = res[-1]
    return float(res)


def seconds_until_next_candle(candle_seconds=None):
    candle_seconds = candle_seconds or CANDLE
    t = time.time()
    nxt = (int(t) // candle_seconds + 1) * candle_seconds
    return nxt - t + 2


def seconds_until_tomorrow_start(cfg):
    now = cairo_now()
    tomorrow = (now + timedelta(days=1)).replace(
        hour=cfg["start_hour"], minute=0, second=0, microsecond=0)
    return (tomorrow - now).total_seconds()


def main(cfg=None):
    STOP.clear()
    set_status(running=True, text="جاري بدء التشغيل")
    provided_cfg = cfg if cfg is not None else load_cfg()
    cfg = {**DEFAULT_CFG, **provided_cfg}

    legacy_strategy = (
        "keltner15"
        if cfg.get("strategy", "keltner15") == "keltner15"
        else "trend_reversal"
    )

    strategy_mode = cfg.get("strategy_mode", legacy_strategy)
    if strategy_mode not in ("both", "keltner15", "trend_reversal"):
        strategy_mode = legacy_strategy

    enabled_strategies = (
        ["keltner15", "trend_reversal"]
        if strategy_mode == "both"
        else [strategy_mode]
    )

    configure_strategy(enabled_strategies[0])

    log(
        f"وضع الاستراتيجيات: {strategy_mode} | "
        f"الأزواج: {cfg.get('pairs_mode')} | "
        f"من {cfg['start_hour']}:00 إلى {cfg['end_hour']}:00 (القاهرة)"
    )

    amounts = cfg["martingale"]
    set_status(text="جاري تسجيل الدخول...")
    api = connect(cfg)
    set_status(balance=api.get_balance())

    st = load_state()

    # استعادة الاستراتيجية للدورات القديمة التي لا تحتوي على هذا الحقل.
    if st.get("pair") and not st.get("cycle_strategy"):
        st["cycle_strategy"] = legacy_strategy
        save_state(st)

    refresh(st, amounts)
    log(
        f"بدء التشغيل | ربح اليوم: {st['pnl']:.2f}$ | "
        f"خطوة المضاعفة: {st['step'] + 1}"
    )

    last_scan_by_strategy = {}

    def wait_for_enabled_candle():
        waits = [
            seconds_until_next_candle(
                900 if strategy == "keltner15" else 300
            )
            for strategy in enabled_strategies
        ]
        nap(max(1, min(waits)))

    while True:
        today = cairo_now().strftime("%Y-%m-%d")

        if st["date"] != today:
            st = new_day_state(today, st)
            save_state(st)
            refresh(st, amounts)
            log("يوم جديد - تصفير ربح اليوم مع استمرار أي دورة مفتوحة")

        if st["stopped"]:
            wait = seconds_until_tomorrow_start(cfg)
            set_status(text=f"متوقف اليوم: {st['stop_reason']}")
            log(
                f"متوقف اليوم ({st['stop_reason']}). "
                f"هيشتغل تاني بكرة بعد {wait/3600:.1f} ساعة"
            )
            nap(min(wait, 600))
            continue

        step = st["step"]
        if step < 0 or step >= len(amounts):
            st["stopped"] = True
            st["stop_reason"] = "مستوى مارتينجال غير صالح"
            save_state(st)
            continue

        # وجود زوج محفوظ يعني أن دورة المارتينجال لم تنتهِ بعد.
        locked = bool(st.get("pair"))

        hour = cairo_now().hour
        in_window = cfg["start_hour"] <= hour < cfg["end_hour"]
        finish_cycle = cfg.get("finish_cycle_after_hours", True)

        if locked:
            cycle_strategy = st.get("cycle_strategy") or legacy_strategy
            configure_strategy(cycle_strategy)

            if not in_window and not finish_cycle:
                set_status(text="خارج وقت التشغيل - دورة المارتينجال محفوظة")
                nap(seconds_until_next_candle(CANDLE))
                continue

            api = ensure_connected(api, cfg)
            if api is None:
                nap(30)
                continue

            payout = pair_payout_if_ready(
                api, cfg, st["pair"], cycle_strategy
            )

            if payout is None:
                st["wait"] = st.get("wait", 0) + 1
                log(
                    f"{st['pair']} غير متاح أو العائد أقل من "
                    f"{int(cfg['min_payout'] * 100)}% "
                    f"(انتظار رقم {st['wait']}) - "
                    f"الدورة مستمرة على نفس الزوج"
                )
                set_status(
                    text=f"انتظار إتاحة {st['pair']} - دورة المارتينجال محفوظة"
                )
                save_state(st)
                refresh(st, amounts)
                nap(seconds_until_next_candle(CANDLE))
                continue

            sig = {
                "pair": st["pair"],
                "direction": st["direction"],
                "streak": "مضاعفة",
                "payout": payout,
                "strategy": cycle_strategy,
            }

        else:
            if not in_window:
                set_status(text="خارج وقت التشغيل")
                nap(30)
                continue

            set_status(text="بانتظار الشمعة وفحص الاستراتيجيات")
            sig = None

            # الأولوية لـ Keltner15 ثم Trend Reversal.
            for strategy in enabled_strategies:
                candle_seconds = 900 if strategy == "keltner15" else 300
                current_bucket = int(time.time() // candle_seconds)

                fresh = (
                    time.time() % candle_seconds
                    <= max(8, cfg["max_entry_delay_sec"] - 15)
                )

                if not fresh:
                    continue

                if last_scan_by_strategy.get(strategy) == current_bucket:
                    continue

                last_scan_by_strategy[strategy] = current_bucket

                api = ensure_connected(api, cfg)
                if api is None:
                    break

                set_status(text=f"جاري فحص استراتيجية {strategy}...")
                sig = find_signal(api, cfg, strategy)

                if sig:
                    break

            if not sig:
                wait_for_enabled_candle()
                continue

        # تثبيت فريم وانتهاء الاستراتيجية التي أصدرت الإشارة.
        configure_strategy(sig["strategy"])

        amount = amounts[step]
        profit = place_and_wait(api, sig, amount,cfg)

        if profit is None:
            if locked:
                nap(seconds_until_next_candle(CANDLE))
            else:
                nap(3)
            continue

        st["wait"] = 0

        if profit > 0:
            result = "WIN"
            st["step"] = 0
            st["pair"] = None
            st["direction"] = None
            st["cycle_strategy"] = None

        elif profit < 0:
            result = "LOSS"
            st["step"] = step + 1
            st["pair"] = sig["pair"]
            st["direction"] = sig["direction"]
            st["cycle_strategy"] = sig["strategy"]

        else:
            result = "DRAW"
            # التعادل يعيد نفس المبلغ ونفس الزوج والاستراتيجية.
            st["pair"] = sig["pair"]
            st["direction"] = sig["direction"]
            st["cycle_strategy"] = sig["strategy"]

        st["pnl"] = round(st["pnl"] + profit, 2)

        log(
            f"النتيجة: {result} | ربح الصفقة: {profit:.2f}$ | "
            f"صافي اليوم: {st['pnl']:.2f}$"
        )

        log_trade([
            cairo_now().strftime("%Y-%m-%d %H:%M:%S"),
            sig["pair"],
            sig["direction"],
            step + 1,
            amount,
            sig["payout"],
            sig["streak"],
            result,
            round(profit, 2),
            st["pnl"],
        ])

        if st["pnl"] >= cfg["daily_profit_target"]:
            st["stopped"] = True
            st["stop_reason"] = (
                f"تحقق هدف الربح اليومي {cfg['daily_profit_target']}$"
            )

        elif st["step"] >= len(amounts):
            st["stopped"] = True
            st["stop_reason"] = "خسرت كل خطوات المضاعفة التسعة"
            st["step"] = 0
            st["pair"] = None
            st["direction"] = None
            st["cycle_strategy"] = None

        save_state(st)
        refresh(st, amounts)

        try:
            set_status(balance=api.get_balance())
        except Exception:
            pass


def run(cfg=None):
    """نقطة التشغيل (بتستخدمها الواجهة): بتمسك الأخطاء وبتكتبها في السجل."""
    try:
        main(cfg)
    except StopBot:
        log("تم إيقاف البوت")
    except Exception as e:
        log(f"خطأ: {e}")
    finally:
        set_status(running=False, text="متوقف")


if __name__ == "__main__":
    try:
        run()
    except KeyboardInterrupt:
        log("تم إيقاف البوت يدويًا")
