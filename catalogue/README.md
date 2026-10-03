# DubaiNow service catalogue, refactored

Draft 1.0, 2 October 2026. Built from `service 20261002-71315.json` (351 records) and Amplitude usage (unique users of Service_Clicked, 2 September to 2 October 2026).

## In this folder

| File | What it's for |
|---|---|
| `dubainow-services.json` | The new catalogue, for design and development: services, things, guides, topics, app features, merged and retired records, and analytics keys. |
| `catalogue-review.csv` | One row per old record, plus the new ones: what it was, what it becomes, the proposed English and Arabic names, and what needs confirming. Opens in Numbers or Excel. |
| `analytics-keys.csv` | Every Amplitude key, the record it belongs to and its new id. |
| `README.md` | This report. |
| `_build/` | The decision for every record (`decisions_*.psv`, `new.psv`, `meta.json`) and the scripts that generate the files above. Change a decision, then run `python3 _build/build.py --write && python3 _build/report.py`. |

## The short version

- **351 records become 219 services**: 210 standalone, 4 bundles and 5 journeys. There are also 10 things (Your vehicles, Your documents and so on), 1 guide and 20 charity options.
- **57 drafts are retired**, 37 records merge into others, and 13 leave the catalogue because they're app features or technical records.
- **Every live entry has a new name**: GOV.UK-style and verb first, with a short name, a one-line description and a proposed Arabic name. Services also have a topic, a sub-topic and an owner; cross-topic bundles sit outside the topics.
- **34 entries need an owner, a scope or a description confirmed** before they ship (see [To confirm](#to-confirm)).
- **The C2 blueprint boards misread 11 analytics codes.** The real catalogue shows what they are (see [Corrections to the C2 boards](#corrections-to-the-c2-boards)).

## What happened to the 351 records

| Decision | Records | What it means |
|---|---:|---|
| Keep | 209 | Stays, under a new name. |
| Publish | 8 | A draft today that's worth launching (find a doctor, find a pharmacy, lost and found, Makani, rent increase, Move home, Salik tag, find a broker). |
| Confirm first | 7 | The scope or owner is unclear. Each has a working name. |
| Becomes an option | 20 | Charities and campaigns. They become options inside Donate to a registered charity instead of 20 separate services. |
| Merge | 37 | Duplicates, virtual copies and pieces of another service (Pay All Fines becomes the Fines view of Pay all). |
| Move out | 13 | App features (Happiness Meter, themes, Around Me, payment settings, the assistant) and technical records. |
| Retire | 57 | Drafts only: old copies, ended campaigns, tests, private marketplaces and things other apps already do. Together they had 76 users. |
| New | 6 | Two bundles and a journey from the C2 blueprints, Donate to a registered charity, and two standalone services split out of today's Getting married and May They Rest In Peace. |

## The three types

The types we agreed are applied to every service. Standalone services stay visible even when a bundle or journey covers them.

| Type | Rule | Badge | How it's named |
|---|---|---|---|
| Standalone | One task, one entity, one sitting. | None. The owner shows beside the name. | A verb and the one thing: Renew your residence visa. |
| Bundle | The same job for many items, done by DubaiNow in one go. | All at once | A verb and the whole set, plus a count line: Pay all your bills and fines, "6 bills and fines, one payment". |
| Journey | A life goal across many entities, over weeks or months. | Start to finish | A verb and the goal, plus a scale line: Build a house, "7 stages · 7 entities · 1 to 2 years". |

### Bundles and journeys in the catalogue

| Name | Type | Today | Count or scale line | Services inside | Users (30 days) |
|---|---|---|---|---:|---:|
| Pay all your bills and fines | Bundle | Pay All | N bills and fines, one payment | 13 | 187.3k |
| Apply for all benefits for people of determination | Bundle | PoD Bundle | N benefits, one application | 3 | 6.1k |
| Get certificates and letters | Bundle | New | N certificates, one request | 21 | – |
| Renew your family's visas | Bundle | New | N visas due, one application | 2 | – |
| Get married | Journey | Getting married | 5 stages · 7 entities · 3 to 12 months | 7 | 7k |
| Take care of things after a death | Journey | May They Rest In Peace | 5 stages · 7 entities · Weeks to months | 5 | 1.5k |
| Build a house | Journey | New | 7 stages · 7 entities · 1 to 2 years | 17 | – |
| Learn to drive | Journey | Drive a Vehicle | 5 stages · 3 entities · 1 to 3 months | 1 | – |
| Move home | Journey | Move to a New Home | 5 stages · 7 entities · 2 to 6 weeks | 10 | – |

Notes:
- **Pay all** absorbs Pay All Fines (46k users) as its Fines view and Auto Payment (7.6k) as a Pay automatically setting. Its 187k is the sum of those keys, so it's an upper bound.
- **Get married** and **Take care of things after a death** exist today but do one thing each (the premarital test, the burial). That task becomes its own standalone service inside the journey, so it doesn't disappear.
- **Change Vehicle Ownership** calls itself a bundle. By our types it's a standalone (one task, one entity, one sitting) that runs its checks inline, so it's named Transfer vehicle ownership. A Buy a car journey is a later candidate.
- **Learn to drive** comes from a draft ("Drive a vehicle"). Most of its stages aren't in DubaiNow yet, so it's marked to confirm.
- Each bundle and journey in the JSON lists the services it needs that DubaiNow doesn't have yet (`notInDubaiNowYet`).

## Naming

The seven GOV.UK rules and the DubaiNow additions from the C1 board apply to every name. Some cases weren't covered there, so this pass adds five rules:

1. **Providers a resident holds an account with are named**: du, e&, Salik, nol, ENOC VIP, Etihad WE. Example: Pay a du bill or recharge. The name is still the task.
2. **"Journey" and "bundle" are reserved for the types** and never appear in a name. The draft Journey Planner, and C1's sample "Plan a journey", would become Plan a trip on public transport.
3. **A standalone never sounds like a life goal.** Buy or Sell Property becomes Transfer property ownership.
4. **The verb list grows.** C1 had 16 starting verbs; the catalogue needs 43, among them Sponsor, Swap, Track, Activate, Connect and Close. Every name still starts with what the resident does.
5. **Ids are verb-first and leave out the entity**: `renew-vehicle-registration`, not `rta.vehicle-registration.renew`. Owners change (parking moved from RTA to Parkin in 2024), and the id shouldn't. The sample record on C1 should follow this.

Arabic names are proposed for every entry, written for the task rather than translated. They need a native editor before anything ships.

### Before and after: the 20 most used

| Users (30 days) | Today | New name | Short name |
|---:|---|---|---|
| 187.3k | Pay All | Pay all your bills and fines | Pay all |
| 70.1k | Check Visa Status | Check your visa status | Visa status |
| 60.6k | Salik Top-up | Top up your Salik account | Top up Salik |
| 59k | DEWA Bill | Pay your electricity and water bill | Pay DEWA |
| 58k | Documents | Your documents | Documents |
| 55.6k | Vehicles | Your vehicles | Vehicles |
| 50.6k | e& | Pay an e& bill or recharge | Pay e& |
| 49.3k | Du | Pay a du bill or recharge | Pay du |
| 46.1k | Manage Family Residency | Your family's residency | Family residency |
| 35.9k | Pay for Parking | Pay for parking | Pay parking |
| 34.1k | Visit Visa | Get a visit visa for a relative or friend | Visit visa |
| 28.2k | Traffic Fines | Pay a traffic fine | Pay traffic fine |
| 25.8k | My Benefits | Your offers and benefits | Benefits |
| 21.1k | Get Family Residency | Sponsor a family member | Sponsor family |
| 19.7k | Pay Residency Fines | Pay a residency fine | Pay visa fine |
| 17.9k | Prayer Times | Check prayer times | Prayer times |
| 17k | My License | Your driving licence | Driving licence |
| 14.9k | Domestic Worker | Sponsor a domestic worker | Domestic worker |
| 13.8k | Official Reports | Get a travel or dependants report | Travel report |
| 12.1k | Renew License | Renew your driving licence | Renew licence |

## Topics

Each service sits in one topic and one sub-topic. Cross-topic bundles (Pay all, Get certificates and letters) sit on Home and in Services, outside the topics.

| Topic | Services | Sub-topics | Main owners |
|---|---:|---|---|
| Driving and transport | 38 | Vehicles (11), Driving licences and permits (7), Fines (4), Parking, Salik and tolls (7), Plate numbers (3), Roads, traffic and accidents (3), Public transport (1), Flights and drones (2) | RTA, Dubai Police, Parkin, Salik and 5 more |
| Visas and residency | 13 | Your visa and residence (7), Family, visitors and domestic workers (4), Emirates ID and passports (2) | GDRFA, ICP |
| Housing and utilities | 57 | Renting (5), Electricity, water, cooling and gas (12), Phone and internet (2), Buying and owning property (8), Building and home works (11), Housing support for citizens (18) | MBRHE, DLD, Dubai Municipality, DEWA and 6 more |
| Health | 13 | Doctors, hospitals and care (6), Health records and vaccinations (4), Health insurance (1), Medical emergencies (2) | DHA, Dubai Ambulance |
| Family and social support | 15 | Financial support (4), People of determination (5), Older citizens (3), Family life, fostering and guardianship (3) | CDA, Awqaf and Minors Affairs Foundation |
| Births, marriages and deaths | 6 | Births (0), Marriage (4), When someone dies (2) | DHA, CDA, Dubai Municipality |
| Work and business | 12 | Your job and pension (5), Running a business (2), Healthcare professionals (4), Money and credit (1) | DHA, Dubai Military Pension Fund, National Service and Reserve Authority, Permanent Committee for Labour Affairs and 3 more |
| Education and learning | 10 | Nurseries and schools (7), Universities, training and scholarships (3) | KHDA, Hamdan Scholarship Programme |
| Faith and giving | 13 | Prayer and mosques (3), Charity and zakat (5), Endowments (3), Religious guidance (2) | IACAD, Awqaf and Minors Affairs Foundation, IACAD-licensed charities |
| City and environment | 17 | Report a problem (1), Waste and sewage (3), Parks, beaches and the outdoors (4), Pets and animals (6), Addresses (1), Municipality fees (1), Post and parcels (1) | Dubai Municipality, Emirates Post |
| Police, justice and the law | 14 | Emergencies (1), Report a crime (5), Police certificates and visits (4), Courts, cases and legal help (4) | Dubai Police, Legal Affairs Department, Economic Security Centre, Dubai Courts |
| Leisure, culture and events | 8 | Events (1), Culture and landmarks (2), Sport (3), Community and volunteering (2), Offers and benefits (0) | Dubai Sports Council, DET, Dubai Culture, CDA |

The C1 taxonomy board says "312 services in 12 topics". Those were placeholder numbers; the real catalogue has 219. Births has no services at all, and Leisure has only 8.

## Analytics clean-up

Amplitude logged 289 different keys for Service_Clicked:

- 239 match a catalogue code exactly, and 2 match apart from spelling (`Patient_Transfer` against `Patient Transfer`).
- 29 are numeric catalogue ids, not codes (`7` is Pay All Fines).
- 14 sit on a code used by more than one record. In 2 of them, both records are live and the users can't be split: `to_whom_it_may_concern` (housing support and financial support letters) and `TWIMC` (military pension and property ownership certificates).
- 3 were mapped by hand. `Death` (1,244 users) matches no record at all.
- 2 are junk: `-1` with 2,007 users, and `(none)`.
- "My Vehicles Essentials" (43.9k users) didn't join to its record, because the catalogue code ends in a space.

Recommendation: log the new id as `service_id` on every event, and keep the old code as `legacy_code` while dashboards move over. Totals that add several keys together are upper bounds, because one person can open more than one.

## Other problems in today's catalogue

- **Names**: one English name is written in Arabic (Construction loan), and many use entity names, Title Case or system words (Smart, Management, Inquiry, Request).
- **Descriptions copied from other services**: Vehicle Renewal shows My Vehicles' text, and Early Settlement shows Replacement Grant's.
- **A broken draft**: "Dewa" (2207) has driving content and shares the code `dewa` with the real DEWA bill.
- **Charity sprawl**: 20 live charity entries, plus 10 seasonal campaigns left behind as drafts.
- **Technical records in the service list**: General Config and Amplitude Event Management.
- **Virtual duplicates**: du prepaid, e& prepaid, Renew Driving License and Manage Health Services each copy a real service.
- **Missing translations**: 16 records have no English or no Arabic name.

## Corrections to the C2 boards

The C2 blueprints were built from analytics codes before the catalogue arrived. These codes were misread:

| Board | Code | What the board says | What it is |
|---|---|---|---|
| Pay all | `violation` (19.7k) | Traffic fines | Pay a residency fine (GDRFA) |
| Pay all | `PublicFines` (4k) | A municipality fine | Pay a public transport fine (RTA) |
| Family visas | `entry_permit` (21.1k) | A visit entry permit | Sponsor a family member (family residency). The board's 55,154 for inviting family to visit should be 34,093 (Visit Visa alone). |
| Certificates | `official_reports` (13.8k) | Police reports | Get a travel or dependants report (GDRFA) |
| Certificates | `TWIMC`, `to_whom_it_may_concern` | One letter type | Four different letters: military pension, property ownership, housing support and financial support. The "4 keys, 8,641 users" group needs redoing. |
| Build a house | `ratifying_inspection`, `housing`, `site_survey`, `approved_drawings`, `UnifiedSPR` | A building inspection, housing eligibility, and general survey and drawing steps | A vehicle inspection from abroad (not part of the journey), Correct your housing fee (Move home), and the plot survey, approved drawings and site plan |

## To confirm

| Entry | Users (30 days) | Status | What to confirm |
|---|---:|---|---|
| Your offers and benefits | 25.8k | live | No description in the catalogue. Confirm what it holds. |
| Get a certificate of your marital status | 4.7k | live | Who owns it. |
| Apply for a residence visa | 3.2k | live | Confirm who it is for. |
| Register as a volunteer | 3.1k | live | Who owns it. |
| Get a parking permit | 3k | live | May overlap with Buy a parking subscription (2228). |
| Report a problem with your employer | 2.5k | live | Who owns it. |
| Book a majlis | 2.1k | live | Who owns it. |
| Complain about an insurance company | 1.6k | live | Who owns it, and whether it covers health insurance only. |
| Find landmarks to visit | 1.6k | live | Who owns it. |
| Find a university | 1.4k | live | Who owns it. |
| Check the status of a property | 1.2k | live | Confirm what status covers. |
| Find the qibla direction | 1.2k | live | Confirm whether this is a compass or a visit to set the qibla in a building. |
| Book a park or beach visit | 977 | live | The catalogue description is only Dubai Parks. |
| Get a certificate of your traffic record | 941 | live | Who owns it. |
| Book an ambulance to move a patient | 911 | live | Who owns it. |
| Join the retirees programme (Thukher) | 877 | live | Who owns it. |
| Check your young child's development | 803 | live | Who owns it. |
| Pay a kaffarah | 759 | live | Who owns it. |
| Passport services (to confirm) | 684 | to confirm | Whether it renews a UAE passport (ICP) or moves a visa to a new passport (GDRFA). |
| Report fraud or an economic crime | 651 | live | Who owns it. |
| Get a certificate that you owe no traffic fines | 628 | live | Who owns it. |
| Choose a ready-made villa design | 498 | live | The catalogue description is only Typical Villa. |
| Edge of Life (to confirm) | 417 | to confirm | No description in the catalogue. Find out what it is. |
| Call an ambulance in an emergency | 285 | live | Who owns it. |
| Get a temporary road to your building site | 267 | live | Who owns it. |
| Get family counselling | 260 | maintenance | Who owns it. |
| Apply to be excused from a rehabilitation certificate | 112 | live | Who owns it. |
| Register as a heart patient for priority ambulance help | 7 | live | Who owns it. |
| Parking benefit (to confirm) | 3 | to confirm | Who owns it. |
| Learn to drive | – | to confirm | What it covers. |
| Get repairs to your new home in its first year | – | to confirm | Draft. Confirm it runs today. |
| Rent a house (to confirm) | – | to confirm | Who owns it. |
| Arrange a burial | – | proposed | Who owns it. |
| Report a human rights violation | – | to confirm | Draft. Confirm the owner and whether it should launch. |

## Not in DubaiNow yet

The bundles and journeys need these services, and none of the 351 records provide them:

- Open a driving file at a driving school (RTA), needed by Learn to drive
- Get an eye test (Opticians), needed by Learn to drive
- Book driving lessons (Driving schools), needed by Learn to drive
- Get your first driving licence (RTA), needed by Learn to drive
- Book a medical fitness test (DHA), needed by Renew your family's visas
- Renew a resident's Emirates ID (ICP), needed by Renew your family's visas
- Renew health insurance (Insurers), needed by Renew your family's visas
- Connect district cooling (Empower), needed by Move home
- Connect gas (Gas providers), needed by Move home
- Get home internet and TV (du or e&), needed by Move home
- Register the plot in your name (DLD), needed by Build a house
- Get a building permit (Dubai Municipality), needed by Build a house
- Get fire safety approval (Dubai Civil Defence), needed by Build a house
- Connect to the sewer network (Dubai Municipality), needed by Build a house
- Get a phone and internet line (du or e&), needed by Build a house
- Get a building completion certificate (Dubai Municipality), needed by Build a house
- Get a parking permit for people of determination (RTA), needed by Apply for all benefits for people of determination
- Register a marriage contract (Dubai Courts), needed by Get married
- Add your spouse to your family book (ICP), needed by Get married
- Apply for the marriage grant (Ministry of Family (to confirm)), needed by Get married
- Get a death certificate (DHA), needed by Take care of things after a death
- Tell government in one go (DubaiNow), needed by Take care of things after a death
- Apply to divide an inheritance (Dubai Courts), needed by Take care of things after a death
- Cancel a residence visa (GDRFA), needed by Take care of things after a death
- Anything under Births: registering a birth or getting a birth certificate

## Next steps

1. Review the decisions and names. `catalogue-review.csv` has a row per old record, sorted by decision and usage.
2. A native editor reviews every Arabic name.
3. Owners confirm the 34 open items.
4. Correct the C2 boards and replace the C1 placeholder counts with these numbers.
5. Design the standalone service flows, starting with the most used.
