#!/usr/bin/env python3
"""Crash sweep: launch every route, sheet and persona item on the emulators and report crashes.
   tools/sweep.py [serial …]   (default: every attached emulator; the debug APK must be installed)"""
import json, re, shlex, subprocess, sys, threading, time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ADB = "/opt/homebrew/share/android-commandlinetools/platform-tools/adb"
PKG = "com.salehtl.dubainownext"
SRC = ROOT / "app/src/main/java/com/salehtl/dubainownext"

cat = json.load(open(ROOT / "app/src/main/assets/dubainow-services.json"))
cases = []  # (label, extras dict)
def add(persona, **extras): cases.append({"persona": persona, **extras})

for p in ["sara", "khalid", "faisal", "wei", "guest"]:
    for tab in ["home", "services", "activity", "search"]: add(p, tab=tab)
    for r in ["allServices", "entities", "bundlesList", "journeysList", "drafts", "history", "notifications",
              "vehicles", "documents", "family"]:
        add(p, tab="services" if r in ("allServices", "entities", "bundlesList", "journeysList") else "activity", route=r)
    for s in ["profile", "happiness", "notifications", "widgetGallery", "editFavorites", "extendParking",
              "topUpSalik", "demoMenu", "comingSoon", "payAll"]:
        add(p, sheet=s)
    add(p, assistant="true")
    for h in ["6", "13", "19", "23"]: add(p, hour=h)

# Every catalogue record (as Sara, plus a guest pass over services)
for t in cat["topics"]:
    add("sara", tab="services", route=f"topic:{t['id']}")
    for st in t.get("subtopics", []):
        sid = st["id"] if isinstance(st, dict) else st
        add("sara", tab="services", route=f"subtopic:{t['id']}/{sid}")
for s in cat["services"] + cat["things"] + cat["guides"]:
    kind = {"bundle": "bundle", "journey": "journey"}.get(s.get("type"), "service")
    add("sara", tab="services", route=f"{kind}:{s['id']}")
    add("guest", tab="services", route=f"{kind}:{s['id']}")
owners = sorted({s.get("owner") or "To confirm" for s in cat["services"]})
for o in owners: add("sara", tab="services", route=f"entity:{o}")

# Persona items: vehicles, documents, requests, receipts
for p in ["sara", "khalid", "faisal", "wei"]:
    text = (SRC / f"data/personas/{p.capitalize()}.kt").read_text()
    for ctor, kind, tab in [("Vehicle(", "vehicle", "activity"), ("DocumentItem(", "document", "activity"),
                            ("RequestItem(", "request", "activity"), ("Receipt(", "receipt", "activity")]:
        for m in re.finditer(re.escape(ctor) + r'\s*(?:id\s*=\s*)?"([^"]+)"', text):
            add(p, tab=tab, route=f"{kind}:{m.group(1)}")
            if kind == "receipt": add(p, sheet=f"receipt:{m.group(1)}")
    for kind in ["vehicle", "document", "request", "receipt"]: add(p, route=f"{kind}:does-not-exist")

def adb(serial, *args, timeout=30):
    return subprocess.run([ADB, "-s", serial, *args], capture_output=True, text=True, timeout=timeout).stdout

def run(serial, work, results):
    while True:
        with lock:
            if not work: return
            case = work.pop(0)
        adb(serial, "shell", "am", "force-stop", PKG)
        adb(serial, "logcat", "-b", "crash", "-c")
        extras = []
        for k, v in case.items(): extras += ["-e", k, shlex.quote(v)]
        adb(serial, "shell", "am", "start", "-n", f"{PKG}/.MainActivity", *extras)
        time.sleep(2.6)
        crash = adb(serial, "logcat", "-d", "-b", "crash")
        alive = adb(serial, "shell", "pidof", PKG).strip()
        if f"Process: {PKG}" in crash or not alive:
            lines = [l for l in crash.splitlines() if "Exception" in l or "Error" in l or "at com.salehtl" in l][:6]
            results.append((case, "\n".join(lines) or "process not running"))
            print("CRASH", case, *lines[:3], sep="\n  ", flush=True)

serials = sys.argv[1:] or [l.split()[0] for l in subprocess.run([ADB, "devices"], capture_output=True, text=True).stdout.splitlines()[1:] if l.strip()]
lock = threading.Lock(); work = list(cases); results = []
print(f"{len(cases)} launches on {len(serials)} emulators", flush=True)
threads = [threading.Thread(target=run, args=(s, work, results)) for s in serials]
[t.start() for t in threads]; [t.join() for t in threads]
print(f"done: {len(cases)} launches, {len(results)} crashes")
for c, e in results: print(c, e, sep="\n  ")
