#!/usr/bin/env python3
"""Write catalogue/README.md from the built catalogue so every number matches the data."""
import json
from collections import Counter

OUT = "/Users/saleh/Developer/Claude Workspace/DubaiNow Next/catalogue"
c = json.load(open(f"{OUT}/dubainow-services.json", encoding="utf-8"))
S, T, G = c["services"], c["things"], c["guides"]
st = c["stats"]
by_id = {x["id"]: x for x in S + T + G}

def k(n):
    return f"{n/1000:.1f}k".replace(".0k", "k") if n >= 1000 else str(n)

def legacy_names(x):
    names = [l["name"] for l in x["legacy"] if l["decision"] in ("keep", "restore", "review")]
    return names[0] if names else (x["legacy"][0]["name"] if x["legacy"] else "New")

keys = c["analyticsKeys"]
how = Counter(e["matchedBy"] for e in keys)
junk = [e for e in keys if e["matchedBy"] == "junk"]
dec = st["decisions"]
live_entries = len(S) + len(T) + len(G)
confirm = sorted([x for x in S + T + G if x.get("toConfirm")], key=lambda x: -x["uniques30d"])
retired_users = sum(r["uniques30d"] for r in c["retired"])

L = []
w = L.append
w("# DubaiNow service catalogue, refactored")
w("")
w("Draft 1.0, 2 October 2026. Built from `service 20261002-71315.json` (351 records) and Amplitude usage "
  "(unique users of Service_Clicked, 2 September to 2 October 2026).")
w("")
w("## In this folder")
w("")
w("| File | What it's for |")
w("|---|---|")
w("| `dubainow-services.json` | The new catalogue, for design and development: services, things, guides, topics, app features, merged and retired records, and analytics keys. |")
w("| `catalogue-review.csv` | One row per old record, plus the new ones: what it was, what it becomes, the proposed English and Arabic names, and what needs confirming. Opens in Numbers or Excel. |")
w("| `analytics-keys.csv` | Every Amplitude key, the record it belongs to and its new id. |")
w("| `README.md` | This report. |")
w("| `_build/` | The decision for every record (`decisions_*.psv`, `new.psv`, `meta.json`) and the scripts that generate the files above. Change a decision, then run `python3 _build/build.py --write && python3 _build/report.py`. |")
w("")
w("## The short version")
w("")
w(f"- **351 records become {st['services']} services**: {st['byType']['standalone']} standalone, "
  f"{st['byType']['bundle']} bundles and {st['byType']['journey']} journeys. There are also {st['things']} things "
  f"(Your vehicles, Your documents and so on), {st['guides']} guide and {st['charityOptions']} charity options.")
w(f"- **{dec['retire']} drafts are retired**, {dec['merge']} records merge into others, and {dec['move']} leave the catalogue "
  "because they're app features or technical records.")
w("- **Every live entry has a new name**: GOV.UK-style and verb first, with a short name, a one-line description "
  "and a proposed Arabic name. Services also have a topic, a sub-topic and an owner; cross-topic bundles sit outside the topics.")
w(f"- **{len(confirm)} entries need an owner, a scope or a description confirmed** before they ship (see [To confirm](#to-confirm)).")
w("- **The C2 blueprint boards misread 11 analytics codes.** The real catalogue shows what they are "
  "(see [Corrections to the C2 boards](#corrections-to-the-c2-boards)).")
w("")
w("## What happened to the 351 records")
w("")
w("| Decision | Records | What it means |")
w("|---|---:|---|")
rows = [
    ("Keep", dec.get("keep", 0), "Stays, under a new name."),
    ("Publish", dec.get("restore", 0), "A draft today that's worth launching (find a doctor, find a pharmacy, lost and found, Makani, rent increase, Move home, Salik tag, find a broker)."),
    ("Confirm first", dec.get("review", 0), "The scope or owner is unclear. Each has a working name."),
    ("Becomes an option", dec.get("fold", 0), "Charities and campaigns. They become options inside Donate to a registered charity instead of 20 separate services."),
    ("Merge", dec.get("merge", 0), "Duplicates, virtual copies and pieces of another service (Pay All Fines becomes the Fines view of Pay all)."),
    ("Move out", dec.get("move", 0), "App features (Happiness Meter, themes, Around Me, payment settings, the assistant) and technical records."),
    ("Retire", dec.get("retire", 0), f"Drafts only: old copies, ended campaigns, tests, private marketplaces and things other apps already do. Together they had {retired_users} users."),
]
for a, b, d in rows:
    w(f"| {a} | {b} | {d} |")
w(f"| New | {st['newRecords']} | Two bundles and a journey from the C2 blueprints, Donate to a registered charity, and two standalone services split out of today's Getting married and May They Rest In Peace. |")
w("")
w("## The three types")
w("")
w("The types we agreed are applied to every service. Standalone services stay visible even when a bundle or journey covers them.")
w("")
w("| Type | Rule | Badge | How it's named |")
w("|---|---|---|---|")
w("| Standalone | One task, one entity, one sitting. | None. The owner shows beside the name. | A verb and the one thing: Renew your residence visa. |")
w("| Bundle | The same job for many items, done by DubaiNow in one go. | All at once | A verb and the whole set, plus a count line: Pay all your bills and fines, \"6 bills and fines, one payment\". |")
w("| Journey | A life goal across many entities, over weeks or months. | Start to finish | A verb and the goal, plus a scale line: Build a house, \"7 stages · 7 entities · 1 to 2 years\". |")
w("")
w("### Bundles and journeys in the catalogue")
w("")
w("| Name | Type | Today | Count or scale line | Services inside | Users (30 days) |")
w("|---|---|---|---|---:|---:|")
meta_services = sorted([x for x in S if x["type"] in ("bundle", "journey")],
                       key=lambda x: (x["type"] != "bundle", -x["uniques30d"], x["name"]))
for x in meta_services:
    today = legacy_names(x)
    line = (x.get("countLine") or x.get("scaleLine")).replace("{n}", "N")
    n = len(x["parts"]) + len(x.get("options", []))
    w(f"| {x['name']} | {x['type'].capitalize()} | {today} | {line} | {n} | {k(x['uniques30d']) if x['uniques30d'] else '–'} |")
w("")
w("Notes:")
w("- **Pay all** absorbs Pay All Fines (46k users) as its Fines view and Auto Payment (7.6k) as a Pay automatically setting. "
  "Its 187k is the sum of those keys, so it's an upper bound.")
w("- **Get married** and **Take care of things after a death** exist today but do one thing each (the premarital test, the burial). "
  "That task becomes its own standalone service inside the journey, so it doesn't disappear.")
w("- **Change Vehicle Ownership** calls itself a bundle. By our types it's a standalone (one task, one entity, one sitting) that runs its checks inline, "
  "so it's named Transfer vehicle ownership. A Buy a car journey is a later candidate.")
w("- **Learn to drive** comes from a draft (\"Drive a vehicle\"). Most of its stages aren't in DubaiNow yet, so it's marked to confirm.")
w("- Each bundle and journey in the JSON lists the services it needs that DubaiNow doesn't have yet (`notInDubaiNowYet`).")
w("")
MULTI = ("Apply for", "Apply to", "Top up", "Work out")
def verb(n):
    return next((m for m in MULTI if n.startswith(m + " ")), n.split()[0])
n_verbs = len({verb(x["name"]) for x in S if "(to confirm)" not in x["name"]})
w("## Naming")
w("")
w("The seven GOV.UK rules and the DubaiNow additions from the C1 board apply to every name. Some cases weren't covered there, so this pass adds five rules:")
w("")
w("1. **Providers a resident holds an account with are named**: du, e&, Salik, nol, ENOC VIP, Etihad WE. Example: Pay a du bill or recharge. The name is still the task.")
w("2. **\"Journey\" and \"bundle\" are reserved for the types** and never appear in a name. The draft Journey Planner, and C1's sample \"Plan a journey\", would become Plan a trip on public transport.")
w("3. **A standalone never sounds like a life goal.** Buy or Sell Property becomes Transfer property ownership.")
w(f"4. **The verb list grows.** C1 had 16 starting verbs; the catalogue needs {n_verbs}, among them Sponsor, Swap, Track, Activate, Connect and Close. Every name still starts with what the resident does.")
w("5. **Ids are verb-first and leave out the entity**: `renew-vehicle-registration`, not `rta.vehicle-registration.renew`. Owners change (parking moved from RTA to Parkin in 2024), and the id shouldn't. The sample record on C1 should follow this.")
w("")
w("Arabic names are proposed for every entry, written for the task rather than translated. They need a native editor before anything ships.")
w("")
w("### Before and after: the 20 most used")
w("")
w("| Users (30 days) | Today | New name | Short name |")
w("|---:|---|---|---|")
top = sorted(S + T, key=lambda x: -x["uniques30d"])[:20]
for x in top:
    w(f"| {k(x['uniques30d'])} | {legacy_names(x)} | {x['name']} | {x['shortName']} |")
w("")
w("## Topics")
w("")
w("Each service sits in one topic and one sub-topic. Cross-topic bundles (Pay all, Get certificates and letters) sit on Home and in Services, outside the topics.")
w("")
w("| Topic | Services | Sub-topics | Main owners |")
w("|---|---:|---|---|")
for t in c["topics"]:
    subs = ", ".join(f"{s['name']} ({s['services']})" for s in t["subtopics"])
    oc = Counter(x["owner"] for x in S if x["topic"] == t["id"] and x["owner"] and x["owner"] != "DubaiNow")
    ranked = [o for o, _ in oc.most_common()]
    owners = ", ".join(ranked[:4]) + (f" and {len(ranked) - 4} more" if len(ranked) > 4 else "")
    w(f"| {t['name']} | {t['services']} | {subs} | {owners} |")
w("")
w("The C1 taxonomy board says \"312 services in 12 topics\". Those were placeholder numbers; the real catalogue has "
  f"{st['services']}. Births has no services at all, and Leisure has only {next(t['services'] for t in c['topics'] if t['id']=='leisure-culture-and-events')}.")
w("")
w("## Analytics clean-up")
w("")
w(f"Amplitude logged {len(keys)} different keys for Service_Clicked:")
w("")
w(f"- {how['code']} match a catalogue code exactly, and {how['code (spelling differs)']} match apart from spelling (`Patient_Transfer` against `Patient Transfer`).")
w(f"- {how['catalogue id']} are numeric catalogue ids, not codes (`7` is Pay All Fines).")
shared_n = how["shared code"] + how["shared code, can't split"]
cant_split = how["shared code, can't split"]
w(f"- {shared_n} sit on a code used by more than one record. "
  f"In {cant_split} of them, both records are live and the users can't be split: "
  "`to_whom_it_may_concern` (housing support and financial support letters) and `TWIMC` (military pension and property ownership certificates).")
w(f"- {how['manual']} were mapped by hand. `Death` (1,244 users) matches no record at all.")
w(f"- {len(junk)} are junk: `-1` with {next(e['uniques30d'] for e in junk if e['key']=='-1'):,} users, and `(none)`.")
w("- \"My Vehicles Essentials\" (43.9k users) didn't join to its record, because the catalogue code ends in a space.")
w("")
w("Recommendation: log the new id as `service_id` on every event, and keep the old code as `legacy_code` while dashboards move over. "
  "Totals that add several keys together are upper bounds, because one person can open more than one.")
w("")
w("## Other problems in today's catalogue")
w("")
w("- **Names**: one English name is written in Arabic (Construction loan), and many use entity names, Title Case or system words (Smart, Management, Inquiry, Request).")
w("- **Descriptions copied from other services**: Vehicle Renewal shows My Vehicles' text, and Early Settlement shows Replacement Grant's.")
w("- **A broken draft**: \"Dewa\" (2207) has driving content and shares the code `dewa` with the real DEWA bill.")
w("- **Charity sprawl**: 20 live charity entries, plus 10 seasonal campaigns left behind as drafts.")
w("- **Technical records in the service list**: General Config and Amplitude Event Management.")
w("- **Virtual duplicates**: du prepaid, e& prepaid, Renew Driving License and Manage Health Services each copy a real service.")
w("- **Missing translations**: 16 records have no English or no Arabic name.")
w("")
w("## Corrections to the C2 boards")
w("")
w("The C2 blueprints were built from analytics codes before the catalogue arrived. These codes were misread:")
w("")
w("| Board | Code | What the board says | What it is |")
w("|---|---|---|---|")
w("| Pay all | `violation` (19.7k) | Traffic fines | Pay a residency fine (GDRFA) |")
w("| Pay all | `PublicFines` (4k) | A municipality fine | Pay a public transport fine (RTA) |")
w("| Family visas | `entry_permit` (21.1k) | A visit entry permit | Sponsor a family member (family residency). The board's 55,154 for inviting family to visit should be 34,093 (Visit Visa alone). |")
w("| Certificates | `official_reports` (13.8k) | Police reports | Get a travel or dependants report (GDRFA) |")
w("| Certificates | `TWIMC`, `to_whom_it_may_concern` | One letter type | Four different letters: military pension, property ownership, housing support and financial support. The \"4 keys, 8,641 users\" group needs redoing. |")
w("| Build a house | `ratifying_inspection`, `housing`, `site_survey`, `approved_drawings`, `UnifiedSPR` | A building inspection, housing eligibility, and general survey and drawing steps | A vehicle inspection from abroad (not part of the journey), Correct your housing fee (Move home), and the plot survey, approved drawings and site plan |")
w("")
w("## To confirm")
w("")
w("| Entry | Users (30 days) | Status | What to confirm |")
w("|---|---:|---|---|")
for x in confirm:
    q = x["toConfirm"]
    q = {"Owner": "Who owns it.", "Scope": "What it covers."}.get(q, q[0].upper() + q[1:])
    w(f"| {x['name']} | {k(x['uniques30d']) if x['uniques30d'] else '–'} | {x['status']} | {q} |")
w("")
w("## Not in DubaiNow yet")
w("")
w("The bundles and journeys need these services, and none of the 351 records provide them:")
w("")
gaps = []
for x in S:
    for g in x.get("notInDubaiNowYet", []):
        gaps.append((g["name"], g["owner"], x["name"]))
    for s2 in x.get("stages", []):
        for g in s2.get("notInDubaiNowYet", []):
            gaps.append((g["name"], g["owner"], x["name"]))
seen = {}
for n, o, where in gaps:
    seen.setdefault((n, o), []).append(where)
for (n, o), where in seen.items():
    w(f"- {n} ({o}), needed by {', '.join(sorted(set(where)))}")
w("- Anything under Births: registering a birth or getting a birth certificate")
w("")
w("## Next steps")
w("")
w("1. Review the decisions and names. `catalogue-review.csv` has a row per old record, sorted by decision and usage.")
w("2. A native editor reviews every Arabic name.")
w(f"3. Owners confirm the {len(confirm)} open items.")
w("4. Correct the C2 boards and replace the C1 placeholder counts with these numbers.")
w("5. Design the standalone service flows, starting with the most used.")
w("")
open(f"{OUT}/README.md", "w", encoding="utf-8").write("\n".join(L))
print("README lines", len(L))
