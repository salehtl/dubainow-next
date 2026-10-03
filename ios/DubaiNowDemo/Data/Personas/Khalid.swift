import Foundation

// Khalid Al Mansoori — Emirati father building the family house (Paper C1 "Home · A journey on
// Home (Khalid)", and beat 1 of the "Four residents" video). He lives in Al Mizhar 1 and is
// building on his plot in Al Khawaneej 2. Numbers agree on every screen:
//   Build a house: month 9, stage 4 of 7 "Design" — Plan done in January, Land in April,
//   Money in July (MBRHE housing loan approved 16 July). Your turn: "Review your villa drawings",
//   sent by his consultant today. Next: Permits (1 to 2 months).
//   Needs you = 2 (1 due this week): the drawings review (journey step, no due date) ·
//   DEWA AED 386.20 due in 5 days (Al Mizhar 1 · Account 2003 5521).
//   Pay all doesn't show (only 1 payable); the Pay all bundle total would be AED 386.20.
//   Notifications: 4, 2 unread (drawings today, DEWA bill). Salik: AED 96 (topped up AED 100
//   22 days ago → AED 146 then). Widgets: Build a house (large), prayer, weather, energy (medium).

extension Personas {
    static var khalid: Persona { Persona(
        id: "khalid",
        firstName: "Khalid",
        lastName: "Al Mansoori",
        initials: "K",
        avatarHue: .sand,
        tagline: "Emirati father building the family house in Al Khawaneej 2. Villa drawings to review.",
        area: "Al Mizhar 1",
        residency: "UAE national",
        emiratesID: "784-1985-•••••••-1",
        phone: "+971 55 ••• 1200",
        assistantPrompts: [
            "What's next for my house?",
            "Show me my villa drawings",
            "How much do I owe?",
            "Top up Salik with AED 100",
        ],
        makeState: { khalidState() }
    ) }

    static func khalidState() -> PersonaState {
        var s = PersonaState()

        // MARK: Payables
        let dewa = Payable(
            id: "khalid-dewa-sep", kind: .bill, title: "Electricity & water bill", entity: "DEWA",
            detail: "Al Mizhar 1 · Account 2003 5521", amount: 386.20, due: DemoDate.day(5),
            reference: "DEWA-2003-5521-0926", symbol: "bolt.fill", hue: .sand,
            serviceID: "pay-electricity-water-bill"
        )

        // MARK: Needs you (the journey step leads, as on Paper)
        s.needsYou = [
            NeedsYouItem(
                id: "ny-house", kind: .journeyStep(journeyID: "build-a-house"), entity: "Build a house",
                symbol: "house.lodge.fill", hue: .sand,
                title: "Review your villa drawings", detail: "Sent by your consultant today",
                due: nil, primaryAction: "Review drawings", secondaryAction: nil,
                progress: StepProgress(current: 4, total: 7)
            ),
            NeedsYouItem(
                id: "ny-dewa", kind: .payment(dewa), entity: "DEWA", symbol: "bolt.fill", hue: .sand,
                title: "Electricity & water bill", detail: "Al Mizhar 1 · Account 2003 5521",
                due: DemoDate.day(5), primaryAction: "Pay now", secondaryAction: "Later"
            ),
        ]

        // MARK: Favorites and widgets (as on Paper)
        s.favorites = [
            FavoriteItem(id: "fav-payall", title: "Pay all", symbol: "creditcard.fill", hue: .sky, target: .service("pay-all")),
            FavoriteItem(id: "fav-salik", title: "Salik", symbol: "road.lanes", hue: .sand, target: .service("top-up-salik-account")),
            FavoriteItem(id: "fav-vehicles", title: "Vehicles", symbol: "car.fill", hue: .rose, target: .thing("your-vehicles")),
            FavoriteItem(id: "fav-donate", title: "Donate", symbol: "heart.fill", hue: .lilac, target: .service("donate-to-charity")),
        ]
        s.widgets = [
            HomeWidget(id: "w-house", kind: .journey("build-a-house"), size: .large),
            HomeWidget(id: "w-prayer", kind: .prayer, size: .small),
            HomeWidget(id: "w-weather", kind: .weather, size: .small),
            HomeWidget(id: "w-energy", kind: .energy, size: .medium),
        ]
        s.widgetSuggestion = nil

        // MARK: Things
        let cruiser = Plate(code: "L", number: "7340")
        s.vehicles = [
            Vehicle(
                id: "khalid-landcruiser", make: "Toyota", model: "Land Cruiser", year: 2023, colour: "Pearl white",
                plate: cruiser, registrationExpiry: DemoDate.fixed(2027, 2, 18), insurer: "Dubai Insurance",
                insuranceExpiry: DemoDate.fixed(2027, 2, 11), lastInspection: DemoDate.fixed(2026, 2, 16),
                salikTag: "Tag ending 5140"
            ),
        ]
        s.documents = [
            DocumentItem(id: "khalid-eid", title: "Emirates ID", number: "784-1985-•••••••-1", expiry: DemoDate.fixed(2030, 5, 12), symbol: "person.text.rectangle.fill", hue: .mint),
            DocumentItem(id: "khalid-passport", title: "Passport", number: "United Arab Emirates · •••••3307", expiry: DemoDate.fixed(2029, 11, 4), symbol: "book.closed.fill", hue: .sand),
            DocumentItem(id: "khalid-familybook", title: "Family book", number: "Khulasat Al Qaid · •••214", expiry: nil, symbol: "person.2.fill", hue: .peach),
            DocumentItem(id: "khalid-licence", title: "Driving licence", number: "Licence ending 2290", expiry: DemoDate.fixed(2034, 3, 9), symbol: "car.fill", hue: .sky),
            DocumentItem(id: "khalid-mulkiya", title: "Vehicle registration", number: "Land Cruiser · L 7340", expiry: DemoDate.fixed(2027, 2, 18), symbol: "doc.text.fill", hue: .sky),
            DocumentItem(id: "khalid-plot", title: "Title deed", number: "Plot in Al Khawaneej 2 · 2245-•••", expiry: nil, symbol: "map.fill", hue: .sand),
        ]
        // UAE nationals: no visas, so nobody is sponsored and there's no Family visas status.
        s.family = [
            FamilyMember(id: "khalid-maryam", name: "Maryam Al Mansoori", relation: "Wife", age: 38, visaExpiry: nil, sponsoredByYou: false, initials: "M", hue: .lilac),
            FamilyMember(id: "khalid-hamdan", name: "Hamdan Al Mansoori", relation: "Son", age: 12, visaExpiry: nil, sponsoredByYou: false, initials: "H", hue: .sky),
            FamilyMember(id: "khalid-fatima", name: "Fatima Al Mansoori", relation: "Daughter", age: 9, visaExpiry: nil, sponsoredByYou: false, initials: "F", hue: .peach),
            FamilyMember(id: "khalid-saif", name: "Saif Al Mansoori", relation: "Son", age: 4, visaExpiry: nil, sponsoredByYou: false, initials: "S", hue: .mint),
        ]
        s.familyVisas = nil
        s.home = HomeInfo(address: "Villa 14, Street 23, Al Mizhar 1", dewaAccount: "2003 5521", ejariExpiry: nil, makani: "29561 87430")
        s.paymentMethods = [.applePay, .card(brand: "Mastercard", last4: "7710")]

        // MARK: Requests
        s.requests = [
            RequestItem(
                id: "khalid-letter", serviceID: "get-housing-support-letter", title: "Get a letter about your housing support", entity: "MBRHE",
                reference: "MBRHE-HL-26-01942", status: .inReview(answerBy: DemoDate.day(2)),
                notice: nil,
                steps: [
                    RequestStep(title: "Ask for the letter", detail: "\(DemoDate.short(DemoDate.day(-1))) · for your consultant, Dar Sidra Engineering", state: .done),
                    RequestStep(title: "MBRHE prepares your letter", detail: "Answer by \(DemoDate.short(DemoDate.day(2)))", state: .current),
                    RequestStep(title: "Download your letter", detail: "We'll add it to your documents", state: .upcoming),
                ],
                startedOn: DemoDate.day(-1), symbol: "house.fill", hue: .sand
            ),
            RequestItem(
                id: "khalid-loan", serviceID: "apply-building-loan", title: "Apply for a loan to build a home", entity: "MBRHE",
                reference: "MBRHE-BL-26-00731", status: .done,
                notice: nil,
                steps: [
                    RequestStep(title: "Apply with your plot and family details", detail: "\(DemoDate.short(DemoDate.fixed(2026, 5, 11))) · title deed found automatically", state: .done),
                    RequestStep(title: "MBRHE checks your application", detail: "\(DemoDate.short(DemoDate.fixed(2026, 6, 24))) · site visit in Al Khawaneej 2", state: .done),
                    RequestStep(title: "Your loan is approved", detail: "\(DemoDate.short(DemoDate.fixed(2026, 7, 16))) · AED 800,000, interest-free", state: .done),
                    RequestStep(title: "Sign the loan agreement", detail: "\(DemoDate.short(DemoDate.fixed(2026, 7, 23))) · paid in stages as you build", state: .done),
                ],
                startedOn: DemoDate.fixed(2026, 5, 11), symbol: "house.fill", hue: .sand
            ),
        ]

        // MARK: Drafts (kept 30 days)
        s.drafts = [
            DraftItem(id: "khalid-draft-zakat", serviceID: "pay-zakat", title: "Work out and pay your zakat", stepsDone: 1, stepsTotal: 3, savedOn: DemoDate.day(-8), symbol: "heart.fill", hue: .lilac),
        ]

        // MARK: History (newest first)
        s.history = [
            Receipt(id: "khalid-r-donate", title: "Donated to Dar Al Ber Society", shortTitle: "Donated to Dar Al Ber Society", entity: "Dar Al Ber Society", amount: 500,
                    date: DemoDate.day(-12, hour: 21, minute: 18), method: .applePay, reference: "DAB-26-310557",
                    rows: [ReceiptRow(label: "For", value: "Family support")], symbol: "heart.fill", hue: .lilac,
                    serviceID: "donate-to-charity"),
            Receipt(id: "khalid-r-salik", title: "Topped up your Salik account", shortTitle: "Topped up Salik", entity: "Salik", amount: 100,
                    date: DemoDate.day(-22, hour: 7, minute: 36), method: .applePay, reference: "SLK-26-4402",
                    rows: [ReceiptRow(label: "New balance", value: "AED 146.00")], symbol: "road.lanes", hue: .sand,
                    serviceID: "top-up-salik-account"),
            Receipt(id: "khalid-r-dewa", title: "Paid your electricity and water bill", shortTitle: "Paid electricity and water", entity: "DEWA", amount: 452.80,
                    date: DemoDate.day(-26, hour: 20, minute: 12), method: .applePay, reference: "DEWA-2003-5521-0826",
                    rows: [ReceiptRow(label: "For", value: "August · Al Mizhar 1")], symbol: "bolt.fill", hue: .sand,
                    serviceID: "pay-electricity-water-bill", rating: .happy),
            Receipt(id: "khalid-r-loan", title: "Applied for a loan to build a home", shortTitle: "Applied for a building loan", entity: "MBRHE", amount: nil,
                    date: DemoDate.fixed(2026, 5, 11, hour: 10), method: nil, reference: "MBRHE-BL-26-00731",
                    rows: [ReceiptRow(label: "Plot", value: "Al Khawaneej 2"), ReceiptRow(label: "Outcome", value: "Approved 16 July 2026")],
                    symbol: "house.fill", hue: .sand, serviceID: "apply-building-loan", rating: .happy),
        ]

        // MARK: Notifications (2 unread → bell badge 2)
        s.notifications = [
            NotificationItem(id: "khalid-n-drawings", title: "Your villa drawings are ready to review",
                             body: "Dar Sidra Engineering sent the drawings for your house in Al Khawaneej 2. Approve them to apply for your building permit.",
                             date: DemoDate.day(0, hour: 8, minute: 30), symbol: "house.lodge.fill", hue: .sand,
                             isRead: false, link: .journey("build-a-house")),
            NotificationItem(id: "khalid-n-dewa", title: "Your September bill is ready",
                             body: "Electricity and water: AED 386.20, due \(DemoDate.long(DemoDate.day(5))).",
                             date: DemoDate.day(-1, hour: 10, minute: 30), symbol: "bolt.fill", hue: .sand,
                             isRead: false, link: nil),
            NotificationItem(id: "khalid-n-letter", title: "MBRHE is preparing your letter",
                             body: "Your housing support letter will be ready by \(DemoDate.long(DemoDate.day(2))).",
                             date: DemoDate.day(-1, hour: 9, minute: 4), symbol: "house.fill", hue: .sand,
                             isRead: true, link: .request("khalid-letter")),
            NotificationItem(id: "khalid-n-donate", title: "Thank you for your donation",
                             body: "AED 500 to Dar Al Ber Society. Your receipt is in History.",
                             date: DemoDate.day(-12, hour: 21, minute: 19), symbol: "heart.fill", hue: .lilac,
                             isRead: true, link: .receipt("khalid-r-donate")),
        ]

        s.linkedAccounts = [
            LinkedAccount(id: "dewa", name: "DEWA", detail: "Electricity and water · 2003 5521", symbol: "bolt.fill", hue: .sand, isLinked: true),
            LinkedAccount(id: "rta", name: "RTA", detail: "1 vehicle and your driving licence", symbol: "car.fill", hue: .sky, isLinked: true),
            LinkedAccount(id: "police", name: "Dubai Police", detail: "Traffic fines", symbol: "shield.fill", hue: .rose, isLinked: true),
            LinkedAccount(id: "salik", name: "Salik", detail: "1 tag", symbol: "road.lanes", hue: .sand, isLinked: true),
            LinkedAccount(id: "mbrhe", name: "MBRHE", detail: "Housing loan · approved July", symbol: "house.fill", hue: .sand, isLinked: true),
            LinkedAccount(id: "dm", name: "Dubai Municipality", detail: "Building drawings and permits", symbol: "hammer.fill", hue: .sand, isLinked: true),
            LinkedAccount(id: "du", name: "du", detail: "Mobile ending 1200", symbol: "phone.fill", hue: .sand, isLinked: true),
            LinkedAccount(id: "eand", name: "e&", detail: "Not linked", symbol: "wifi", hue: .sand, isLinked: false),
        ]

        // MARK: Live things for widgets
        s.salikBalance = 96
        s.nolBalance = nil
        s.parking = nil
        s.energy = EnergyReading(monthLabel: "September", kWh: 2180, changeVsLastMonth: -16,
                                 comparison: "Lower than 57% of similar homes in Al Mizhar",
                                 lastSixMonths: [1960, 2540, 2890, 3020, 2610, 2180])
        s.metro = nil
        s.journeys = [
            JourneyProgress(journeyID: "build-a-house", stageIndex: 3, headline: "Review your villa drawings",
                            waitingOn: "Sent by your consultant today",
                            startedOn: DemoDate.fixed(2026, 1, 25), destination: "in Al Khawaneej 2",
                            needsYou: true),
        ]
        return s
    }
}
