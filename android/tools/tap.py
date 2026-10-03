#!/usr/bin/env python3
"""Tap a UI element by its text or content description (uses uiautomator's accessibility dump).
   tools/tap.py <serial> "Pay all"            first element whose text/desc contains the label
   tools/tap.py <serial> "=Pay now"           exact match
   tools/tap.py <serial> --list               print every labelled element"""
import re, subprocess, sys, time
ADB = "/opt/homebrew/share/android-commandlinetools/platform-tools/adb"
serial, label = sys.argv[1], sys.argv[2]
def adb(*a): return subprocess.run([ADB, "-s", serial, *a], capture_output=True, text=True).stdout
for attempt in range(3):
    adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    xml = adb("shell", "cat", "/sdcard/ui.xml")
    if "<node" in xml: break
    time.sleep(1)
nodes = []
for m in re.finditer(r'<node [^>]*>', xml):
    n = m.group(0)
    text = re.search(r' text="([^"]*)"', n).group(1)
    desc = re.search(r' content-desc="([^"]*)"', n).group(1)
    b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n)
    x1, y1, x2, y2 = map(int, b.groups())
    if text or desc: nodes.append((text, desc, (x1 + x2) // 2, (y1 + y2) // 2, n))
if label == "--list":
    for t, d, x, y, _ in nodes: print(f"{x:5},{y:5}  {t!r} {d!r}")
    sys.exit(0)
exact = label.startswith("="); want = label[1:] if exact else label.lower()
for t, d, x, y, _ in nodes:
    if (exact and want in (t, d)) or (not exact and (want in t.lower() or want in d.lower())):
        adb("shell", "input", "tap", str(x), str(y)); print(f"tapped {t or d!r} at {x},{y}"); sys.exit(0)
print(f"not found: {label}"); sys.exit(1)
