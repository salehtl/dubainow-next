import Foundation

// Sara Haddad — the main demo persona (Paper C1 scenario).
// Expat mother of two in Al Barsha 1, on a residence visa she is renewing, moving to
// Jumeirah Village Circle. Numbers agree on every screen:
//   Needs you = 4 (2 due this week): DEWA AED 412.60 due tomorrow · passport upload for the
//   visa (step 3 of 5, due in 2 days) · speeding fine AED 600 (blocks the car renewal) ·
//   Patrol registration due in 12 days.
//   Pay all = DEWA + fine = AED 1,012.60.  Renewal through the assistant = AED 420 + 600 = AED 1,020.
//   Salik: topped up AED 100 thirteen days ago (balance then AED 118), AED 18 left now.

extension Personas {
    static var sara: Persona { Persona(
        id: "sara",
        firstName: "Sara",
        lastName: "Haddad",
        initials: "S",
        avatarHue: .peach,
        tagline: "Mum of two in Al Barsha. Car registration due, visa renewal in progress, moving home.",
        area: "Al Barsha 1",
        residency: "Residence visa",
        emiratesID: "784-1990-•••••••-3",
        phone: "+971 50 ••• 4821",
        assistantPrompts: [
            "Renew my car registration",
            "How much do I owe?",
            "When will my visa renewal be done?",
            "Top up Salik with AED 100",
        ],
        makeState: { saraState() }
    ) }

    static func saraState() -> PersonaState {
        var s = PersonaState()

        // MARK: Payables
        let dewa = Payable(
            id: "sara-dewa-sep", kind: .bill, title: "Electricity & water bill", entity: "DEWA",
            detail: "Al Barsha 1 · Account 2004 1187", amount: 412.60, due: DemoDate.day(1),
            reference: "DEWA-2004-1187-0926", symbol: "bolt.fill", hue: .sand,
            serviceID: "pay-electricity-water-bill"
        )
        let fine = Payable(
            id: "sara-fine-speeding", kind: .fine, title: "Speeding fine", entity: "Dubai Police",
            detail: "Sheikh Zayed Road · \(DemoDate.short(DemoDate.day(-18)))", amount: 600, due: nil,
            reference: "DXB-TF-26-3318042", symbol: "gauge.with.dots.needle.67percent", hue: .sky,
            serviceID: "pay-traffic-fine", note: "Blocks your car renewal"
        )

        // MARK: Needs you (order: by due date, the fine just before the renewal it blocks)
        s.needsYou = [
            NeedsYouItem(
                id: "ny-dewa", kind: .payment(dewa), entity: "DEWA", symbol: "bolt.fill", hue: .sand,
                title: "Electricity & water bill", detail: "Al Barsha 1 · Account 2004 1187",
                due: DemoDate.day(1), primaryAction: "Pay now", secondaryAction: "Later"
            ),
            NeedsYouItem(
                id: "ny-visa", kind: .requestStep(requestID: "sara-visa"), entity: "GDRFA",
                symbol: "person.text.rectangle.fill", hue: .lilac,
                title: "Upload a clearer passport copy", detail: "Renew your residence visa",
                due: DemoDate.day(2), primaryAction: "Upload now", secondaryAction: "Details",
                progress: StepProgress(current: 3, total: 5)
            ),
            NeedsYouItem(
                id: "ny-fine", kind: .payment(fine), entity: "Dubai Police",
                symbol: "gauge.with.dots.needle.67percent", hue: .sky,
                title: "Speeding fine", detail: "Sheikh Zayed Road · blocks your car renewal",
                due: nil, primaryAction: "Pay now", secondaryAction: "Details"
            ),
            NeedsYouItem(
                id: "ny-patrol", kind: .renewal(serviceID: "renew-vehicle-registration", thingID: "sara-patrol"),
                entity: "RTA", symbol: "car.fill", hue: .sky,
                title: "Renew your Patrol's registration", detail: "M 48213 · expires \(DemoDate.short(DemoDate.day(12)))",
                due: DemoDate.day(12), primaryAction: "Renew", secondaryAction: "Later"
            ),
        ]

        // MARK: Favorites and widgets
        s.favorites = [
            FavoriteItem(id: "fav-parking", title: "Pay parking", symbol: "p.square.fill", hue: .sky, target: .service("pay-for-parking")),
            FavoriteItem(id: "fav-salik", title: "Salik", symbol: "road.lanes", hue: .sand, target: .service("top-up-salik-account")),
            FavoriteItem(id: "fav-vehicles", title: "Vehicles", symbol: "car.fill", hue: .rose, target: .thing("your-vehicles")),
            FavoriteItem(id: "fav-donate", title: "Donate", symbol: "heart.fill", hue: .lilac, target: .service("donate-to-charity")),
        ]
        s.widgets = [
            HomeWidget(id: "w-parking", kind: .parking, size: .small),
            HomeWidget(id: "w-salik", kind: .salik, size: .small),
            HomeWidget(id: "w-prayer", kind: .prayer, size: .small),
            HomeWidget(id: "w-weather", kind: .weather, size: .small),
            HomeWidget(id: "w-metro", kind: .metro, size: .medium),
            HomeWidget(id: "w-energy", kind: .energy, size: .medium),
        ]
        s.widgetSuggestion = .familyVisas

        // MARK: Things
        let patrol = Plate(code: "M", number: "48213")
        s.vehicles = [
            Vehicle(
                id: "sara-patrol", make: "Nissan", model: "Patrol", year: 2022, colour: "White", plate: patrol,
                registrationExpiry: DemoDate.day(12), insurer: "Sukoon Insurance",
                insuranceExpiry: DemoDate.fixed(2027, 3, 14), lastInspection: DemoDate.day(-12),
                salikTag: "Tag ending 3391", fineIDs: ["sara-fine-speeding"]
            ),
            Vehicle(
                id: "sara-corolla", make: "Toyota", model: "Corolla", year: 2019, colour: "Silver",
                plate: Plate(code: "K", number: "30577"), registrationExpiry: DemoDate.fixed(2027, 5, 9),
                insurer: "Sukoon Insurance", insuranceExpiry: DemoDate.fixed(2027, 5, 2),
                lastInspection: DemoDate.fixed(2026, 5, 6), salikTag: "Tag ending 8820"
            ),
        ]
        s.documents = [
            DocumentItem(id: "sara-eid", title: "Emirates ID", number: "784-1990-•••••••-3", expiry: DemoDate.fixed(2028, 3, 21), symbol: "person.text.rectangle.fill", hue: .mint),
            DocumentItem(id: "sara-visa-doc", title: "Residence visa", number: "201/2023/•••••42", expiry: DemoDate.day(47), status: .renewing, statusNote: "Renewing · your turn", symbol: "person.text.rectangle", hue: .lilac),
            DocumentItem(id: "sara-passport", title: "Passport", number: "Lebanon · RL••••821", expiry: DemoDate.fixed(2031, 6, 30), symbol: "book.closed.fill", hue: .sand),
            DocumentItem(id: "sara-licence", title: "Driving licence", number: "Licence ending 6630", expiry: DemoDate.fixed(2036, 9, 22), status: .renewing, statusNote: "New card arrives \(DemoDate.weekday(DemoDate.day(2)))", symbol: "car.fill", hue: .sky),
            DocumentItem(id: "sara-mulkiya", title: "Vehicle registration", number: "Patrol · M 48213", expiry: DemoDate.day(12), status: .expiringSoon, statusNote: "Expires in 12 days", symbol: "doc.text.fill", hue: .sky),
            DocumentItem(id: "sara-ejari", title: "Tenancy contract", number: "Ejari · Al Barsha 1", expiry: DemoDate.fixed(2027, 1, 14), symbol: "key.fill", hue: .sand),
        ]
        s.family = [
            FamilyMember(id: "sara-ahmed", name: "Ahmed Haddad", relation: "Husband", age: 41, visaExpiry: DemoDate.fixed(2027, 8, 3), sponsoredByYou: true, initials: "A", hue: .sky),
            FamilyMember(id: "sara-layla", name: "Layla Haddad", relation: "Daughter", age: 9, visaExpiry: DemoDate.day(73), sponsoredByYou: true, initials: "L", hue: .peach),
            FamilyMember(id: "sara-omar", name: "Omar Haddad", relation: "Son", age: 6, visaExpiry: DemoDate.day(73), sponsoredByYou: true, initials: "O", hue: .mint),
        ]
        s.familyVisas = FamilyVisasStatus(memberIDs: ["sara-ahmed", "sara-layla", "sara-omar"], dueMemberIDs: ["sara-layla", "sara-omar"], expiresOn: DemoDate.day(73))
        s.home = HomeInfo(address: "Villa 23, Street 15, Al Barsha 1", dewaAccount: "2004 1187", ejariExpiry: DemoDate.fixed(2027, 1, 14), makani: "17845 41205")
        s.paymentMethods = [.applePay, .card(brand: "Visa", last4: "4021")]

        // MARK: Requests in progress
        s.requests = [
            RequestItem(
                id: "sara-visa", serviceID: "renew-residence-visa", title: "Renew your residence visa", entity: "GDRFA",
                reference: "GDRFA-R-26-0042817", status: .yourTurn(due: DemoDate.day(2)),
                notice: ("We need a clearer passport copy",
                         "The last photo was blurry around the passport number. Upload a new one by \(DemoDate.long(DemoDate.day(2))) and GDRFA will review your application within 2 working days."),
                steps: [
                    RequestStep(title: "Pass your medical fitness test", detail: "\(DemoDate.short(DemoDate.day(-8))) · DHA, Al Barsha centre", state: .done),
                    RequestStep(title: "Confirm your health insurance", detail: "\(DemoDate.short(DemoDate.day(-8))) · found automatically, valid to 2027", state: .done),
                    RequestStep(title: "Upload your passport copy", detail: "Your turn · by \(DemoDate.long(DemoDate.day(2)))", state: .current),
                    RequestStep(title: "Pay the fees", detail: "About AED 1,150 · we'll show the exact amount", state: .upcoming),
                    RequestStep(title: "GDRFA reviews your application", detail: "Usually 2 working days · we'll notify you", state: .upcoming),
                ],
                startedOn: DemoDate.day(-9), symbol: "person.text.rectangle.fill", hue: .lilac, action: "Upload now"
            ),
            RequestItem(
                id: "sara-licence", serviceID: "renew-driving-licence", title: "Renew your driving licence", entity: "RTA",
                reference: "RTA-DL-26-118204", status: .approved(note: "card arrives \(DemoDate.weekday(DemoDate.day(2)))"),
                notice: nil,
                steps: [
                    RequestStep(title: "Pass an eye test", detail: "\(DemoDate.short(DemoDate.day(-11))) · Al Barsha optician", state: .done),
                    RequestStep(title: "Pay the fee", detail: "\(DemoDate.short(DemoDate.day(-10))) · AED 300", state: .done),
                    RequestStep(title: "RTA approves your renewal", detail: "\(DemoDate.short(DemoDate.day(-1)))", state: .done),
                    RequestStep(title: "Your new card is delivered", detail: "Arrives \(DemoDate.weekday(DemoDate.day(2))) · Emirates Post", state: .current),
                ],
                startedOn: DemoDate.day(-11), symbol: "person.text.rectangle.fill", hue: .sky
            ),
            RequestItem(
                id: "sara-salik-dispute", serviceID: "top-up-salik-account", title: "Dispute a Salik charge", entity: "Salik",
                reference: "SLK-DSP-26-5512", status: .inReview(answerBy: DemoDate.day(7)),
                notice: nil,
                steps: [
                    RequestStep(title: "Tell Salik what happened", detail: "\(DemoDate.short(DemoDate.day(-3))) · charged twice at Al Safa", state: .done),
                    RequestStep(title: "Salik reviews the trips", detail: "Answer by \(DemoDate.short(DemoDate.day(7)))", state: .current),
                    RequestStep(title: "Refund to your Salik balance", detail: "AED 4 if approved", state: .upcoming),
                ],
                startedOn: DemoDate.day(-3), symbol: "road.lanes", hue: .sand
            ),
        ]

        // MARK: Drafts (kept 30 days)
        s.drafts = [
            DraftItem(id: "sara-draft-ejari", serviceID: "register-tenancy-contract", title: "Register your tenancy contract (Ejari)", stepsDone: 2, stepsTotal: 4, savedOn: DemoDate.day(-4), symbol: "key.fill", hue: .sand),
            DraftItem(id: "sara-draft-conduct", serviceID: "get-good-conduct-certificate", title: "Get a certificate of good conduct", stepsDone: 1, stepsTotal: 3, savedOn: DemoDate.day(-6), symbol: "checkmark.shield.fill", hue: .rose),
        ]

        // MARK: History (newest first)
        s.history = [
            Receipt(id: "sara-r-parking", title: "Paid for parking", shortTitle: "Paid for parking", entity: "Parkin", amount: 34,
                    date: DemoDate.day(-4, hour: 18, minute: 2), method: .applePay, reference: "PKN-26-77120",
                    rows: [ReceiptRow(label: "Sessions", value: "6 in Al Barsha and DIFC")], symbol: "p.square.fill", hue: .sky,
                    serviceID: "pay-for-parking", count: 6),
            Receipt(id: "sara-r-licence", title: "Applied to renew your driving licence", shortTitle: "Applied to renew driving licence", entity: "RTA", amount: 300,
                    date: DemoDate.day(-10, hour: 20, minute: 41), method: .card(brand: "Visa", last4: "4021"), reference: "RTA-DL-26-118204",
                    rows: [], symbol: "person.text.rectangle.fill", hue: .sky, serviceID: "renew-driving-licence"),
            Receipt(id: "sara-r-salik", title: "Topped up your Salik account", shortTitle: "Topped up Salik", entity: "Salik", amount: 100,
                    date: DemoDate.day(-13, hour: 8, minute: 14), method: .applePay, reference: "SLK-26-7731",
                    rows: [ReceiptRow(label: "New balance", value: "AED 118.00")], symbol: "road.lanes", hue: .sand,
                    serviceID: "top-up-salik-account"),
            Receipt(id: "sara-r-dewa", title: "Paid your electricity and water bill", shortTitle: "Paid electricity and water", entity: "DEWA", amount: 398.20,
                    date: DemoDate.day(-28, hour: 21, minute: 5), method: .applePay, reference: "DEWA-2004-1187-0826",
                    rows: [ReceiptRow(label: "For", value: "August · Al Barsha 1")], symbol: "bolt.fill", hue: .sand,
                    serviceID: "pay-electricity-water-bill", rating: .happy),
        ]

        // MARK: Notifications (2 unread → bell badge 2)
        s.notifications = [
            NotificationItem(id: "sara-n-visa", title: "Upload a clearer passport copy",
                             body: "GDRFA needs a new photo of your passport page by \(DemoDate.long(DemoDate.day(2))).",
                             date: DemoDate.day(0, hour: 8, minute: 2), symbol: "person.text.rectangle.fill", hue: .lilac,
                             isRead: false, link: .request("sara-visa")),
            NotificationItem(id: "sara-n-dewa", title: "Your September bill is ready",
                             body: "Electricity and water: AED 412.60, due tomorrow.",
                             date: DemoDate.day(-3, hour: 10, minute: 30), symbol: "bolt.fill", hue: .sand,
                             isRead: false, link: nil),
            NotificationItem(id: "sara-n-licence", title: "Driving licence renewal approved",
                             body: "Your new card arrives on \(DemoDate.weekday(DemoDate.day(2))).",
                             date: DemoDate.day(-1, hour: 16, minute: 12), symbol: "checkmark.seal.fill", hue: .mint,
                             isRead: true, link: .request("sara-licence")),
            NotificationItem(id: "sara-n-salik", title: "Your Salik balance is low",
                             body: "AED 18 left, about 4 trips.",
                             date: DemoDate.day(-1, hour: 7, minute: 40), symbol: "road.lanes", hue: .sand,
                             isRead: true, link: nil),
        ]

        s.linkedAccounts = [
            LinkedAccount(id: "dewa", name: "DEWA", detail: "Electricity and water · 2004 1187", symbol: "bolt.fill", hue: .sand, isLinked: true),
            LinkedAccount(id: "rta", name: "RTA", detail: "2 vehicles and your driving licence", symbol: "car.fill", hue: .sky, isLinked: true),
            LinkedAccount(id: "police", name: "Dubai Police", detail: "Traffic fines", symbol: "shield.fill", hue: .rose, isLinked: true),
            LinkedAccount(id: "salik", name: "Salik", detail: "2 tags", symbol: "road.lanes", hue: .sand, isLinked: true),
            LinkedAccount(id: "parkin", name: "Parkin", detail: "Parking for 2 vehicles", symbol: "p.square.fill", hue: .sky, isLinked: true),
            LinkedAccount(id: "du", name: "du", detail: "Mobile ending 4821", symbol: "phone.fill", hue: .sand, isLinked: true),
            LinkedAccount(id: "eand", name: "e&", detail: "Not linked", symbol: "wifi", hue: .sand, isLinked: false),
        ]

        // MARK: Live things for widgets
        s.salikBalance = 18
        s.nolBalance = 46.50
        s.parking = ParkingSession(zone: "365C", area: "Al Barsha", plate: patrol, startedAt: DemoDate.minutes(-78), endsAt: DemoDate.minutes(42), ratePerHour: 4)
        s.energy = EnergyReading(monthLabel: "September", kWh: 1284, changeVsLastMonth: -12,
                                 comparison: "Lower than 68% of similar homes in Al Barsha",
                                 lastSixMonths: [1610, 1702, 1745, 1690, 1459, 1284])
        s.metro = MetroStation(name: "Business Bay", line: "Red Line", departures: [
            MetroDeparture(towards: "Centrepoint", inMinutes: 3, nextInMinutes: 9),
            MetroDeparture(towards: "Expo 2020", inMinutes: 6, nextInMinutes: 14),
        ])
        s.journeys = [
            JourneyProgress(journeyID: "move-home", stageIndex: 1, headline: "Sign the tenancy",
                            waitingOn: "Waiting for your landlord to sign · sent yesterday",
                            startedOn: DemoDate.day(-4), destination: "to Jumeirah Village Circle"),
        ]
        return s
    }
}
