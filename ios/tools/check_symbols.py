#!/usr/bin/env python3
"""List SF Symbol-looking string literals in the Swift sources that don't exist in the system symbol set."""
import pathlib, plistlib, re, sys
ROOT = pathlib.Path(__file__).resolve().parent.parent / "DubaiNowDemo"
RES = pathlib.Path("/System/Library/CoreServices/CoreGlyphs.bundle/Contents/Resources")
names = set(plistlib.load(open(RES / "name_availability.plist", "rb"))["symbols"])
raw = (RES / "name_aliases.strings").read_bytes()
aliases = raw.decode("utf-16") if raw[:2] in (b"\xff\xfe", b"\xfe\xff") else raw.decode("utf-8", "ignore")
pat = re.compile(r'(?:systemName:|systemImage:|symbol:|Image\(systemName:)\s*"([a-z0-9]+(?:\.[a-z0-9]+)*)"')
lit = re.compile(r'"([a-z0-9]+(?:\.[a-z0-9]+)+)"')
bad = {}
for f in ROOT.rglob("*.swift"):
    text = f.read_text()
    for m in set(pat.findall(text)) | set(lit.findall(text)):
        if m not in names and f'"{m}"' not in aliases and not m.startswith(("com.", "en_")):
            bad.setdefault(m, []).append(f.relative_to(ROOT).as_posix())
for k, v in sorted(bad.items()):
    print(f"{k}: {', '.join(sorted(set(v)))}")
sys.exit(1 if bad else 0)
