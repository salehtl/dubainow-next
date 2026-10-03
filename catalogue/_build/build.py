#!/usr/bin/env python3
"""Build the refactored DubaiNow service catalogue from the hand-written decision files."""
import csv, json, os, re, sys
from collections import Counter, defaultdict

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = "/Users/saleh/Developer/Claude Workspace/DubaiNow Next/catalogue"
WRITE = "--write" in sys.argv

FIELDS = ["id", "decision", "entry", "type", "topic", "subtopic", "owner", "audience", "slug",
          "name_en", "short_en", "desc_en", "name_ar", "part_of", "merge_into", "aliases", "notes"]

TOPICS = [
    ("transport", "driving-and-transport", "Driving and transport", "القيادة والمواصلات", [
        ("vehicles", "Vehicles"), ("licences", "Driving licences and permits"), ("fines", "Fines"),
        ("parking", "Parking, Salik and tolls"), ("plates", "Plate numbers"),
        ("roads", "Roads, traffic and accidents"), ("public-transport", "Public transport"),
        ("air", "Flights and drones")]),
    ("visas", "visas-and-residency", "Visas and residency", "التأشيرات والإقامة", [
        ("your-visa", "Your visa and residence"), ("family", "Family, visitors and domestic workers"),
        ("ids", "Emirates ID and passports")]),
    ("housing", "housing-and-utilities", "Housing and utilities", "السكن والمرافق", [
        ("renting", "Renting"), ("utilities", "Electricity, water, cooling and gas"),
        ("phone-internet", "Phone and internet"), ("owning", "Buying and owning property"),
        ("building", "Building and home works"), ("support", "Housing support for citizens")]),
    ("health", "health", "Health", "الصحة", [
        ("care", "Doctors, hospitals and care"), ("records", "Health records and vaccinations"),
        ("insurance", "Health insurance"), ("emergencies", "Medical emergencies")]),
    ("family", "family-and-social-support", "Family and social support", "الأسرة والدعم الاجتماعي", [
        ("financial", "Financial support"), ("pod", "People of determination"),
        ("older", "Older citizens"), ("family-life", "Family life, fostering and guardianship")]),
    ("life", "births-marriages-and-deaths", "Births, marriages and deaths", "المواليد والزواج والوفاة", [
        ("births", "Births"), ("marriage", "Marriage"), ("death", "When someone dies")]),
    ("work", "work-and-business", "Work and business", "العمل والأعمال", [
        ("your-job", "Your job and pension"), ("business", "Running a business"),
        ("health-pros", "Healthcare professionals"), ("money", "Money and credit")]),
    ("education", "education-and-learning", "Education and learning", "التعليم والتعلّم", [
        ("schools", "Nurseries and schools"), ("higher", "Universities, training and scholarships")]),
    ("faith", "faith-and-giving", "Faith and giving", "الشؤون الدينية والعطاء", [
        ("prayer", "Prayer and mosques"), ("giving", "Charity and zakat"),
        ("endowments", "Endowments"), ("guidance", "Religious guidance")]),
    ("city", "city-and-environment", "City and environment", "المدينة والبيئة", [
        ("report", "Report a problem"), ("waste", "Waste and sewage"),
        ("parks", "Parks, beaches and the outdoors"), ("animals", "Pets and animals"),
        ("streets", "Addresses"), ("payments", "Municipality fees"), ("post", "Post and parcels")]),
    ("justice", "police-justice-and-the-law", "Police, justice and the law", "الشرطة والقضاء والقانون", [
        ("emergencies", "Emergencies"), ("report", "Report a crime"),
        ("police", "Police certificates and visits"), ("courts", "Courts, cases and legal help")]),
    ("leisure", "leisure-culture-and-events", "Leisure, culture and events", "الترفيه والثقافة والفعاليات", [
        ("events", "Events"), ("culture", "Culture and landmarks"), ("sport", "Sport"),
        ("community", "Community and volunteering"), ("offers", "Offers and benefits")]),
]
TOPIC = {t[0]: t for t in TOPICS}
SUB = {t[0]: dict(t[4]) for t in TOPICS}

VERBS = ["Apply for", "Apply to", "Apply", "Renew", "Register", "Pay", "Get", "Book", "Report", "Check",
         "Change", "Replace", "Transfer", "Cancel", "Top up", "Dispute", "Find", "Work out"]
EXTRA_VERBS = ["Activate", "Ask", "Adopt", "Buy", "Call", "Choose", "Close", "Complain", "Confirm",
               "Connect", "Correct", "Declare", "Donate", "End", "Join", "Move", "Share", "Sign",
               "Sponsor", "Swap", "Take", "Track", "Update", "Build", "Learn", "Arrange", "Send"]
BANNED = ["smart", "online", "portal", "happiness", "services", "service", "manage", "submit",
          "process", "request", "inquiry", "enquiry", "application", "bundle", "journey", "e-services"]

DECISIONS = {"keep", "restore", "review", "merge", "retire", "move", "fold", "new"}
LIVE = {"keep", "restore", "review", "new"}

def owner_split(o):
    o = o.strip()
    confirmed = True
    if o.endswith("(to confirm)"):
        o = o[: -len("(to confirm)")].strip(); confirmed = False
    if o in ("", "Owner to confirm"):
        return None, False
    return o, confirmed

def norm(k):
    return re.sub(r"[\s_\-]+", "_", k.strip().lower())

def parse(path):
    rows = []
    for n, line in enumerate(open(path, encoding="utf-8"), 1):
        line = line.rstrip("\n")
        if not line.strip():
            continue
        p = line.split("|")
        d = p[1] if len(p) > 1 else ""
        if len(p) == 17:
            r = dict(zip(FIELDS, p))
        elif d == "retire" and len(p) == 3:
            r = {"id": p[0], "decision": d, "notes": p[2]}
        elif d == "merge" and len(p) == 5:
            r = {"id": p[0], "decision": d, "merge_into": p[2], "aliases": p[3], "notes": p[4]}
        elif d == "move" and len(p) == 4:
            r = {"id": p[0], "decision": d, "entry": p[2], "notes": p[3]}
        else:
            sys.exit(f"{os.path.basename(path)}:{n}: bad field count {len(p)} for decision {d!r}: {line[:90]}")
        for f in FIELDS:
            r.setdefault(f, "")
            r[f] = r[f].strip()
        r["_src"] = f"{os.path.basename(path)}:{n}"
        rows.append(r)
    return rows

legacy = {r["id"]: r for r in json.load(open(os.path.join(HERE, "extract.json"), encoding="utf-8"))}
legacy["1111107"]["en_name"] = "Around Me"   # language detection picked the Spanish slot
usage = json.load(open(os.path.join(HERE, "usage.json"), encoding="utf-8"))
meta = json.load(open(os.path.join(HERE, "meta.json"), encoding="utf-8"))

rows = []
for f in ["decisions_1.psv", "decisions_2.psv", "decisions_3.psv", "new.psv"]:
    rows += parse(os.path.join(HERE, f))

errors, warns, infos = [], [], []
def E(m): errors.append(m)
def W(m): warns.append(m)

# --- coverage -------------------------------------------------------------
seen = Counter(r["id"] for r in rows if r["decision"] != "new")
for i, c in seen.items():
    if i not in legacy: E(f"unknown legacy id {i}")
    if c > 1: E(f"legacy id {i} decided {c} times")
for i in legacy:
    if i not in seen: E(f"legacy id {i} ({legacy[i]['code']}) has no decision")

# --- per-row checks ---------------------------------------------------------
slugs = {}
for r in rows:
    d = r["decision"]
    if d not in DECISIONS: E(f"{r['_src']} bad decision {d}")
    if d in LIVE or d == "fold":
        s = r["slug"]
        if not s: E(f"{r['_src']} missing slug"); continue
        if not re.fullmatch(r"[a-z0-9]+(-[a-z0-9]+)*", s): E(f"{r['_src']} bad slug {s}")
        if s in slugs: E(f"duplicate slug {s} ({slugs[s]['_src']} and {r['_src']})")
        slugs[s] = r
        if not r["name_en"]: E(f"{r['_src']} missing name")
        if r["entry"] not in ("service", "thing", "info", "option"): E(f"{r['_src']} bad entry {r['entry']}")
        if r["entry"] == "service" and r["type"] not in ("standalone", "bundle", "journey"):
            E(f"{r['_src']} service without type")
        t, st = r["topic"], r["subtopic"]
        if t and t not in TOPIC: E(f"{r['_src']} unknown topic {t}")
        if t and st and st not in SUB.get(t, {}): E(f"{r['_src']} unknown subtopic {t}/{st}")
        if r["entry"] == "service" and r["type"] == "standalone" and not (t and st):
            if not r["slug"].endswith("-review"): W(f"{s}: standalone without topic/subtopic")
        name = r["name_en"]
        if len(name) > 60: E(f"{s}: name over 60 characters ({len(name)})")
        if name.endswith("."): W(f"{s}: name ends with a full stop")
        if "&" in name.replace("e&", ""): W(f"{s}: ampersand in name")
        if r["entry"] in ("service",) and not r["slug"].endswith("-review"):
            lw = name.lower().replace("national service", "national-svc")
            for b in BANNED:
                if re.search(rf"\b{re.escape(b)}\b", lw):
                    W(f"{s}: banned word '{b}' in name: {name}")
            if r["type"] != "journey" and re.search(r"\bjourney\b", lw): E(f"{s}: 'journey' in a non-journey name")
            v = next((v for v in VERBS + EXTRA_VERBS if name == v or name.startswith(v + " ")), None)
            if not v: W(f"{s}: name doesn't start with a known verb: {name}")
            elif v in EXTRA_VERBS: infos.append(v)
            words = [w for w in re.findall(r"[A-Za-z][A-Za-z']*", name)[1:]]
            caps = [w for w in words if w[0].isupper()]
            if len(words) >= 3 and len(caps) / len(words) > 0.5: W(f"{s}: looks like Title Case: {name}")
        if r["entry"] == "thing" and not name.startswith("Your "): W(f"{s}: thing name should start with 'Your'")
        if r["entry"] in ("service", "thing") and not r["short_en"]: E(f"{s}: missing short name")
        if r["short_en"] and len(r["short_en"]) > 20: W(f"{s}: short name over 20 characters: {r['short_en']}")
        if not r["name_ar"]: W(f"{s}: missing Arabic name")
        if not r["desc_en"]: W(f"{s}: missing description")
    if d == "merge" and not r["merge_into"]: E(f"{r['_src']} merge without target")

# --- references ---------------------------------------------------------------
for r in rows:
    if r["decision"] == "merge":
        t = r["merge_into"]
        if t not in slugs: E(f"{r['_src']} merges into unknown {t}")
        elif slugs[t]["decision"] not in LIVE: E(f"{r['_src']} merges into non-live {t}")
    for p in [x.strip() for x in r["part_of"].split(",") if x.strip()]:
        if p not in slugs: E(f"{r['_src']} part_of unknown {p}")
        else:
            pr = slugs[p]
            if r["decision"] == "fold":
                pass
            elif pr.get("type") not in ("bundle", "journey"):
                E(f"{r['_src']} part_of {p}, which is not a bundle or journey")
for k, m in meta.items():
    if k not in slugs: E(f"meta for unknown {k}"); continue
    for st in m.get("stages", []):
        for p in st.get("parts", []):
            if p not in slugs: E(f"meta {k}: stage part unknown {p}")
            elif slugs[p]["decision"] not in LIVE: E(f"meta {k}: stage part not live {p}")

# --- final id for each legacy record ----------------------------------------------
def resolve(s, depth=0):
    r = slugs[s]
    if r["decision"] == "merge" and depth < 5:
        return resolve(r["merge_into"], depth + 1)
    return s

by_id = {r["id"]: r for r in rows if r["decision"] != "new"}
def final_of(r):
    d = r["decision"]
    if d in LIVE: return r["slug"]
    if d == "merge": return resolve(r["merge_into"])
    if d == "fold": return r["part_of"].split(",")[0].strip()
    return None

# --- analytics keys -----------------------------------------------------------------
code_index = defaultdict(list)
for i, lr in legacy.items():
    code_index[norm(lr["code"])].append(i)
MANUAL = {}
mpath = os.path.join(HERE, "manual_keys.json")
if os.path.exists(mpath):
    MANUAL = json.load(open(mpath, encoding="utf-8"))

key_rows = []
legacy_users = defaultdict(int)       # legacy id -> uniques attributed
final_users = defaultdict(int)        # final entry -> uniques (each key counted once per entry)
shared_note = defaultdict(list)
for k, v in usage.items():
    kk = k.strip()
    entry = {"key": kk, "uniques": v, "legacy": [], "how": "", "note": ""}
    if kk in ("-1", "(none)"):
        entry.update(how="junk", note="Not a service; an empty or default event value.")
    elif kk in MANUAL:
        entry.update(legacy=[MANUAL[kk]["id"]] if MANUAL[kk]["id"] else [], how="manual", note=MANUAL[kk]["note"])
    elif kk.isdigit() and kk in legacy:
        entry.update(legacy=[kk], how="catalogue id")
    else:
        c = code_index.get(norm(kk), [])
        if not c:
            entry.update(how="unmatched")
        elif len(c) == 1:
            entry.update(legacy=c, how="code" if legacy[c[0]]["code"].strip() == kk else "code (spelling differs)")
        else:
            live = [i for i in c if by_id[i]["decision"] in LIVE]
            landed = [i for i in c if final_of(by_id[i])]
            if len(live) == 1:
                pick = live
            elif not live and len({final_of(by_id[i]) for i in landed}) == 1:
                pick = landed[:1] if len(landed) > 1 and len({final_of(by_id[i]) for i in landed}) == 1 else landed
            else:
                pick = []
            if pick and len({final_of(by_id[i]) for i in pick}) == 1:
                others = [i for i in c if i not in pick]
                entry.update(legacy=pick, how="shared code",
                             note="Code also on " + ", ".join(f"{i} ({by_id[i]['decision']})" for i in others)
                             + ("; counted against " + pick[0] if others else ""))
            else:
                pick = live or landed or c
                entry.update(legacy=pick, how="shared code, can't split",
                             note="Same code on " + ", ".join(pick) + ". Counted in full against each, so treat as an upper bound.")
    for i in entry["legacy"]:
        legacy_users[i] += v
    for fin in {final_of(by_id[i]) for i in entry["legacy"]} - {None}:
        final_users[fin] += v
    finals = sorted({final_of(by_id[i]) or f"({by_id[i]['decision']})" for i in entry["legacy"]})
    entry["becomes"] = ", ".join(finals)
    key_rows.append(entry)

unmatched = [e for e in key_rows if e["how"] == "unmatched"]

# --- report -------------------------------------------------------------------------------
print("rows", len(rows), "legacy", len(legacy))
print("decisions", Counter(r["decision"] for r in rows))
print("live entries", Counter((r["entry"], r["type"]) for r in rows if r["decision"] in LIVE))
print("ERRORS", len(errors)); [print("  E", e) for e in errors]
print("WARNINGS", len(warns)); [print("  W", w) for w in warns]
print("extra verbs used", Counter(infos).most_common())
print("unmatched keys", [(e["key"], e["uniques"]) for e in unmatched])
if errors:
    sys.exit(1)
if not WRITE:
    sys.exit(0)

# ======================================================================================
# Outputs
# ======================================================================================
os.makedirs(OUT, exist_ok=True)

def listify(s):
    return [x.strip() for x in s.split(",") if x.strip()]

def check_text(r):
    m = re.search(r"CHECK:?\s*(.*)$", r["notes"])
    return m.group(1).strip() if m else ""

def clean_notes(r):
    return re.sub(r"\s*CHECK:?.*$", "", r["notes"]).strip()

status_map = {"published": "live", "beta": "beta", "maintenance": "maintenance", "draft": "draft"}

# legacy records that land on each final entry
lands = defaultdict(list)
for r in rows:
    if r["decision"] == "new":
        continue
    f = final_of(r)
    if f:
        lands[f].append(r["id"])

def legacy_block(ids):
    out = []
    for i in ids:
        lr = legacy[i]; r = by_id[i]
        out.append({"id": i, "code": lr["code"].strip(), "status": lr["status"], "virtual": lr["virtual"],
                    "name": lr["en_name"], "nameAr": lr["ar_name"], "decision": r["decision"],
                    "uniques30d": legacy_users.get(i, 0)})
    return out

def usage_of(slug):
    return final_users.get(slug, 0)

def status_of(r):
    d = r["decision"]
    if d == "new": return "proposed"
    if d == "restore": return "to publish"
    if d == "review": return "to confirm"
    return status_map.get(legacy[r["id"]]["status"], legacy[r["id"]]["status"])

live = [r for r in rows if r["decision"] in LIVE]
parts_of = defaultdict(list)
for r in live:
    for p in listify(r["part_of"]):
        parts_of[p].append(r["slug"])
options_of = defaultdict(list)
for r in rows:
    if r["decision"] == "fold":
        options_of[r["part_of"]].append(r)

def entity_set(slug):
    ents = []
    def add(o):
        o2, _ = owner_split(o)
        if o2 and o2 != "DubaiNow" and o2 not in ents:
            ents.append(o2)
    m = meta.get(slug, {})
    if m.get("stages"):
        for st in m["stages"]:
            for p in st.get("parts", []): add(slugs[p]["owner"])
            for g in st.get("gaps", []): add(g["owner"])
    else:
        for p in parts_of.get(slug, []): add(slugs[p]["owner"])
        for g in m.get("gaps", []): add(g["owner"])
    return ents

def record(r):
    owner, confirmed = owner_split(r["owner"])
    t = r["topic"]
    rec = {
        "id": r["slug"],
        "kind": r["entry"],
    }
    if r["entry"] == "service":
        rec["type"] = r["type"]
    rec.update({
        "name": r["name_en"],
        "nameAr": r["name_ar"] or None,
        "shortName": r["short_en"] or None,
        "description": r["desc_en"] or None,
        "topic": TOPIC[t][1] if t else None,
        "subtopic": r["subtopic"] or None,
        "owner": owner,
        "ownerConfirmed": confirmed,
        "audience": r["audience"] or "all",
        "aliases": listify(r["aliases"]),
        "partOf": listify(r["part_of"]),
        "status": status_of(r),
        "uniques30d": usage_of(r["slug"]),
    })
    if r["entry"] == "service" and r["type"] in ("bundle", "journey"):
        m = meta.get(r["slug"], {})
        ents = entity_set(r["slug"])
        if r["type"] == "bundle":
            parts = parts_of.get(r["slug"], [])
            n = len(parts) + len(options_of.get(r["slug"], []))
            rec["parts"] = parts
            rec["countLine"] = m.get("countLine", "{n} items, one go")
            rec["countLineNote"] = "{n} is filled in for each resident at runtime."
            rec["covers"] = f"{len(parts)} services" + (f" and {len(options_of.get(r['slug'], []))} options" if options_of.get(r["slug"]) else "")
            rec["doesTheWork"] = m.get("doesTheWork")
            if m.get("views"): rec["views"] = m["views"]
            if m.get("settings"): rec["settings"] = m["settings"]
            if m.get("gaps"): rec["notInDubaiNowYet"] = m["gaps"]
        else:
            stages = m.get("stages", [])
            rec["stages"] = [{"name": s["name"], "takes": s.get("takes"), "parts": s.get("parts", []),
                              "notInDubaiNowYet": s.get("gaps", [])} for s in stages]
            inside = []
            for s in stages:
                for p in s.get("parts", []):
                    if p not in inside: inside.append(p)
            for p in parts_of.get(r["slug"], []):
                if p not in inside: inside.append(p)
            rec["parts"] = inside
            rec["lasts"] = m.get("lasts")
            rec["scaleLine"] = f"{len(stages)} stages · {len(ents)} entities · {m.get('lasts', '')}".strip(" ·")
        rec["entities"] = ents
    if r["entry"] == "service" and options_of.get(r["slug"]):
        rec["options"] = [{"id": o["slug"], "name": o["name_en"], "nameAr": o["name_ar"],
                           "description": o["desc_en"], "owner": owner_split(o["owner"])[0],
                           "legacyId": o["id"], "uniques30d": legacy_users.get(o["id"], 0)}
                          for o in options_of[r["slug"]]]
    rec["legacy"] = legacy_block(lands.get(r["slug"], []))
    chk = check_text(r)
    if r["decision"] == "review" or chk or not confirmed:
        rec["toConfirm"] = chk or ("Owner" if not confirmed else "Scope")
    note = clean_notes(r)
    if note: rec["notes"] = note
    return rec

TYPE_ORDER = {"journey": 0, "bundle": 1, "standalone": 2}
def sort_key(r):
    t = r["topic"] or "zz"
    order = [x[0] for x in TOPICS]
    ti = order.index(t) if t in order else 99
    subs = [s[0] for s in TOPIC[t][4]] if t in TOPIC else []
    si = subs.index(r["subtopic"]) if r["subtopic"] in subs else -1
    return (ti, TYPE_ORDER.get(r["type"], 3), si, r["name_en"].lower())

services = [record(r) for r in sorted([r for r in live if r["entry"] == "service"], key=sort_key)]
things = [record(r) for r in sorted([r for r in live if r["entry"] == "thing"], key=sort_key)]
guides = [record(r) for r in sorted([r for r in live if r["entry"] == "info"], key=sort_key)]

app_features = [{"legacyId": r["id"], "code": legacy[r["id"]]["code"].strip(), "name": legacy[r["id"]]["en_name"],
                 "uniques30d": legacy_users.get(r["id"], 0), "note": clean_notes(r) or r["notes"]}
                for r in rows if r["decision"] == "move"]
retired = [{"legacyId": r["id"], "code": legacy[r["id"]]["code"].strip(), "name": legacy[r["id"]]["en_name"],
            "status": legacy[r["id"]]["status"], "uniques30d": legacy_users.get(r["id"], 0), "reason": r["notes"]}
           for r in rows if r["decision"] == "retire"]
merged = [{"legacyId": r["id"], "code": legacy[r["id"]]["code"].strip(), "name": legacy[r["id"]]["en_name"],
           "status": legacy[r["id"]]["status"], "into": resolve(r["merge_into"]),
           "uniques30d": legacy_users.get(r["id"], 0), "note": clean_notes(r)}
          for r in rows if r["decision"] == "merge"]

# topic counts
topic_out = []
for key, slug, name, name_ar, subs in TOPICS:
    in_topic = [s for s in services if s["topic"] == slug]
    topic_out.append({
        "id": slug, "name": name, "nameAr": name_ar,
        "services": len(in_topic),
        "things": [x["id"] for x in things if x["topic"] == slug],
        "journeys": [s["id"] for s in in_topic if s["type"] == "journey"],
        "bundles": [s["id"] for s in in_topic if s["type"] == "bundle"],
        "owners": sorted({s["owner"] for s in in_topic if s["owner"] and s["owner"] != "DubaiNow"}),
        "subtopics": [{"id": sid, "name": sname,
                       "services": len([s for s in in_topic if s["subtopic"] == sid])} for sid, sname in subs],
    })

stats = {
    "legacyRecords": len(legacy),
    "decisions": dict(Counter(r["decision"] for r in rows if r["decision"] != "new")),
    "newRecords": len([r for r in rows if r["decision"] == "new"]),
    "services": len(services),
    "byType": dict(Counter(s["type"] for s in services)),
    "things": len(things), "guides": len(guides),
    "charityOptions": sum(len(v) for v in options_of.values()),
    "appFeatures": len(app_features), "retired": len(retired), "merged": len(merged),
    "toConfirm": len([x for x in services + things + guides if x.get("toConfirm")]),
}

catalogue = {
    "name": "DubaiNow service catalogue",
    "version": "1.0 draft",
    "date": "2026-10-02",
    "source": {
        "catalogue": "service 20261002-71315.json (351 records)",
        "usage": "Amplitude, unique users of Service_Clicked, 2 Sep to 2 Oct 2026",
    },
    "rules": {
        "types": {
            "standalone": "One task, one entity, one sitting. Named with a verb and the one thing. No badge; the owner shows beside the name.",
            "bundle": "The same job done for many items at once, by DubaiNow. Named with a verb and the whole set, with a count line. Badge: All at once.",
            "journey": "A life goal across many entities over weeks or months. Named with a verb and the goal, with a scale line. Badge: Start to finish.",
        },
        "kinds": {
            "service": "Something the resident does.",
            "thing": "One of the resident's things (vehicles, documents, properties). A page, not a service; named Your ...",
            "guide": "Information to read.",
            "option": "A choice inside a service, such as a charity inside Donate to a registered charity.",
        },
        "naming": [
            "Use the words residents use.",
            "Describe the task, not the technology.",
            "Start with a verb.",
            "Leave out entity names; the owner is shown beside the name. Providers a resident holds an account with (du, e&, Salik, nol, ENOC) are named.",
            "Keep the name when the owner or system changes.",
            "No branding or marketing words, and never Journey or Bundle in a name.",
            "Sentence case, under 60 characters, no full stop.",
            "Your for things they have, a for new things.",
            "Local words in brackets only if residents say them: Register your tenancy contract (Ejari).",
            "When a bundle and a standalone give the same result, the standalone takes the narrower name.",
            "Arabic is written, not translated, and a native editor signs off both.",
        ],
        "ids": "Ids are verb-first and leave out the entity, so they survive a change of owner (parking moved from RTA to Parkin in 2024).",
    },
    "stats": stats,
    "topics": topic_out,
    "services": services,
    "things": things,
    "guides": guides,
    "appFeatures": app_features,
    "merged": merged,
    "retired": retired,
    "analyticsKeys": [{"key": e["key"], "uniques30d": e["uniques"], "legacyIds": e["legacy"],
                       "becomes": e["becomes"], "matchedBy": e["how"], "note": e["note"]} for e in key_rows],
}
with open(os.path.join(OUT, "dubainow-services.json"), "w", encoding="utf-8") as f:
    json.dump(catalogue, f, ensure_ascii=False, indent=2)

# --- review sheet: one row per legacy record, plus new records ---------------------------
cols = ["Legacy id", "Legacy code", "Legacy status", "Virtual", "Legacy name", "Legacy Arabic name", "Users (30 days)",
        "Decision", "Becomes", "Kind", "Type", "New name", "Short name", "Description", "Arabic name (proposed)",
        "Topic", "Sub-topic", "Owner", "Audience", "Part of", "Also called", "To confirm", "Notes"]
DEC_LABEL = {"keep": "Keep", "restore": "Publish (draft today)", "review": "Confirm first", "merge": "Merge",
             "retire": "Retire", "move": "Move to app", "fold": "Becomes an option", "new": "New"}
out_rows = []
for r in rows:
    lr = legacy.get(r["id"]) if r["decision"] != "new" else None
    t = r["topic"]
    owner, confirmed = owner_split(r["owner"])
    out_rows.append({
        "Legacy id": r["id"] if lr else "",
        "Legacy code": lr["code"].strip() if lr else "",
        "Legacy status": lr["status"] if lr else "",
        "Virtual": ("yes" if lr["virtual"] else "") if lr else "",
        "Legacy name": lr["en_name"] if lr else "",
        "Legacy Arabic name": lr["ar_name"] if lr else "",
        "Users (30 days)": legacy_users.get(r["id"], 0) if lr else usage_of(r["slug"]),
        "Decision": DEC_LABEL[r["decision"]],
        "Becomes": final_of(r) or ("app feature" if r["decision"] == "move" else ""),
        "Kind": {"service": "Service", "thing": "Thing", "info": "Guide", "option": "Option"}.get(r["entry"], ""),
        "Type": r["type"].capitalize() if r["type"] else "",
        "New name": r["name_en"] if r["decision"] in LIVE or r["decision"] == "fold" else "",
        "Short name": r["short_en"],
        "Description": r["desc_en"],
        "Arabic name (proposed)": r["name_ar"],
        "Topic": TOPIC[t][2] if t else "",
        "Sub-topic": SUB[t].get(r["subtopic"], "") if t else "",
        "Owner": (owner or "") + ("" if confirmed or not owner else " (to confirm)"),
        "Audience": r["audience"],
        "Part of": r["part_of"],
        "Also called": r["aliases"],
        "To confirm": check_text(r) or ("Owner" if (r["decision"] in LIVE and not confirmed) else ""),
        "Notes": clean_notes(r) if r["decision"] in LIVE or r["decision"] == "fold" else r["notes"],
    })
dec_order = {"New": 0, "Keep": 1, "Publish (draft today)": 2, "Confirm first": 3, "Becomes an option": 4,
             "Merge": 5, "Move to app": 6, "Retire": 7}
out_rows.sort(key=lambda x: (dec_order[x["Decision"]], -int(x["Users (30 days)"] or 0), x["New name"] or x["Legacy name"]))
with open(os.path.join(OUT, "catalogue-review.csv"), "w", encoding="utf-8-sig", newline="") as f:
    w = csv.DictWriter(f, fieldnames=cols); w.writeheader(); w.writerows(out_rows)

with open(os.path.join(OUT, "analytics-keys.csv"), "w", encoding="utf-8-sig", newline="") as f:
    w = csv.writer(f)
    w.writerow(["Amplitude key", "Users (30 days)", "Catalogue record(s)", "Becomes", "Matched by", "Note"])
    for e in sorted(key_rows, key=lambda e: -e["uniques"]):
        w.writerow([e["key"], e["uniques"], " ".join(e["legacy"]), e["becomes"], e["how"], e["note"]])

json.dump({"stats": stats, "unmatched": [(e["key"], e["uniques"]) for e in unmatched]},
          open(os.path.join(HERE, "last_stats.json"), "w"), indent=1)
print("WROTE", OUT)
print(json.dumps(stats, indent=1))
