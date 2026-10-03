import Foundation

// Wei Zhang — site engineer from Chengdu living in Al Nahda 2, on a residence visa through his
// employer (Crestline Contracting LLC). No car: he takes the Green Line from Al Nahda and uses a
// nol card. Assistant showcase: he asks in Mandarin about two months of unpaid salary and is led to
// "Report a problem with your employer" (report-employer-problem). Numbers agree on every screen:
//   Needs you = 1 (1 due this week): du mobile bill AED 125.00 due in 5 days (mobile ending 6618).
//   Pay all doesn't show (only 1 payable); the Pay all bundle total would be AED 125.00.
//   nol: AED 14.50 (low, about 4 trips; topped up AED 50 eighteen days ago → AED 61.50 then).
//   Notifications: 3, 2 unread (du bill, low nol). Salik: none (no car).
//   Widgets: nol, weather (Al Mamzar Beach), metro (Al Nahda, Green Line, medium), energy (medium).

extension Personas {
    static var wei: Persona { Persona(
        id: "wei",
        firstName: "Wei",
        lastName: "Zhang",
        initials: "W",
        avatarHue: .mint,
        tagline: "Site engineer from Chengdu in Al Nahda. Takes the metro, asks the assistant in Mandarin.",
        area: "Al Nahda 2",
        residency: "Residence visa",
        emiratesID: "784-1993-•••••••-4",
        phone: "+971 52 ••• 6618",
        assistantPrompts: [
            "我的工资两个月没发了，怎么办？",
            "Top up my nol card with AED 50",
            "Check my visa status",
            "When is the next metro?",
        ],
        makeState: { weiState() }
    ) }

    static func weiState() -> PersonaState {
        var s = PersonaState()

        // MARK: Payables
        let du = Payable(
            id: "wei-du-sep", kind: .bill, title: "Mobile bill", entity: "du",
            detail: "Mobile ending 6618", amount: 125.00, due: DemoDate.day(5),
            reference: "DU-M-6618-0926", symbol: "phone.fill", hue: .sand,
            serviceID: "pay-du-bill"
        )

        // MARK: Needs you
        s.needsYou = [
            NeedsYouItem(
                id: "ny-du", kind: .payment(du), entity: "du", symbol: "phone.fill", hue: .sand,
                title: "Mobile bill", detail: "Mobile ending 6618",
                due: DemoDate.day(5), primaryAction: "Pay now", secondaryAction: "Later"
            ),
        ]

        // MARK: Favorites and widgets
        s.favorites = [
            FavoriteItem(id: "fav-nol", title: "Top up nol", symbol: "tram.fill", hue: .sky, target: .service("top-up-nol-card")),
            FavoriteItem(id: "fav-visa", title: "Visa status", symbol: "person.text.rectangle.fill", hue: .lilac, target: .service("check-visa-status")),
            FavoriteItem(id: "fav-employer", title: "Worker's Voice", symbol: "exclamationmark.bubble.fill", hue: .sand, target: .service("report-employer-problem")),
            FavoriteItem(id: "fav-du", title: "Pay du", symbol: "phone.fill", hue: .sand, target: .service("pay-du-bill")),
        ]
        s.widgets = [
            HomeWidget(id: "w-nol", kind: .nol, size: .small),
            HomeWidget(id: "w-weather", kind: .weather, size: .small),
            HomeWidget(id: "w-metro", kind: .metro, size: .medium),
            HomeWidget(id: "w-energy", kind: .energy, size: .medium),
        ]
        s.widgetSuggestion = nil

        // MARK: Things (no vehicles)
        s.vehicles = []
        s.documents = [
            DocumentItem(id: "wei-eid", title: "Emirates ID", number: "784-1993-•••••••-4", expiry: DemoDate.fixed(2027, 6, 21), symbol: "person.text.rectangle.fill", hue: .mint),
            DocumentItem(id: "wei-visa-doc", title: "Residence visa", number: "201/2025/•••••19", expiry: DemoDate.fixed(2027, 6, 21), symbol: "person.text.rectangle", hue: .lilac),
            DocumentItem(id: "wei-passport", title: "Passport", number: "China · E••••8126", expiry: DemoDate.fixed(2033, 4, 2), symbol: "book.closed.fill", hue: .sand),
            DocumentItem(id: "wei-workpermit", title: "Work permit", number: "Crestline Contracting · ••••7720", expiry: DemoDate.fixed(2027, 6, 21), symbol: "briefcase.fill", hue: .sand),
            DocumentItem(id: "wei-insurance", title: "Health insurance card", number: "Orient Insurance · ••••3391", expiry: DemoDate.fixed(2027, 6, 21), symbol: "cross.case.fill", hue: .mint),
        ]
        // His wife and daughter live in Chengdu; nobody in Dubai is on his visa.
        s.family = []
        s.familyVisas = nil
        s.home = HomeInfo(address: "Flat 506, Building 7, Al Nahda 2", dewaAccount: "2061 0934", ejariExpiry: DemoDate.fixed(2027, 3, 31), makani: "33718 92045")
        s.paymentMethods = [.applePay, .card(brand: "Mastercard", last4: "3306")]

        // MARK: Requests
        s.requests = [
            RequestItem(
                id: "wei-bins", serviceID: "report-city-problem", title: "Report overflowing bins on your street", entity: "Dubai Municipality",
                reference: "DM-RP-26-771204", status: .inReview(answerBy: DemoDate.day(2)),
                notice: nil,
                steps: [
                    RequestStep(title: "Tell Dubai Municipality what's wrong", detail: "\(DemoDate.short(DemoDate.day(-1))) · Street 5, Al Nahda 2, with a photo", state: .done),
                    RequestStep(title: "A team checks the street", detail: "Answer by \(DemoDate.short(DemoDate.day(2)))", state: .current),
                    RequestStep(title: "The bins are emptied", detail: "We'll let you know", state: .upcoming),
                ],
                startedOn: DemoDate.day(-1), symbol: "exclamationmark.bubble.fill", hue: .mint
            ),
        ]

        // MARK: Drafts (kept 30 days)
        s.drafts = [
            DraftItem(id: "wei-draft-beach", serviceID: "book-park-visit", title: "Book a park or beach visit", stepsDone: 1, stepsTotal: 2, savedOn: DemoDate.day(-3), symbol: "tree.fill", hue: .mint),
        ]

        // MARK: History (newest first)
        s.history = [
            Receipt(id: "wei-r-dewa", title: "Paid your electricity and water bill", shortTitle: "Paid electricity and water", entity: "DEWA", amount: 341.90,
                    date: DemoDate.day(-2, hour: 21, minute: 37), method: .applePay, reference: "DEWA-2061-0934-0926",
                    rows: [ReceiptRow(label: "For", value: "September · Al Nahda 2")], symbol: "bolt.fill", hue: .sand,
                    serviceID: "pay-electricity-water-bill", rating: .happy),
            Receipt(id: "wei-r-nol", title: "Topped up your nol card", shortTitle: "Topped up nol", entity: "RTA", amount: 50,
                    date: DemoDate.day(-18, hour: 7, minute: 21), method: .applePay, reference: "NOL-26-339170",
                    rows: [ReceiptRow(label: "New balance", value: "AED 61.50")], symbol: "tram.fill", hue: .sky,
                    serviceID: "top-up-nol-card"),
            Receipt(id: "wei-r-du", title: "Paid your du bill", shortTitle: "Paid du", entity: "du", amount: 125.00,
                    date: DemoDate.day(-30, hour: 20, minute: 44), method: .applePay, reference: "DU-M-6618-0826",
                    rows: [ReceiptRow(label: "For", value: "August · mobile ending 6618")], symbol: "phone.fill", hue: .sand,
                    serviceID: "pay-du-bill"),
            Receipt(id: "wei-r-nol-aug", title: "Topped up your nol card", shortTitle: "Topped up nol", entity: "RTA", amount: 50,
                    date: DemoDate.day(-41, hour: 7, minute: 18), method: .applePay, reference: "NOL-26-301884",
                    rows: [ReceiptRow(label: "New balance", value: "AED 58.00")], symbol: "tram.fill", hue: .sky,
                    serviceID: "top-up-nol-card"),
        ]

        // MARK: Notifications (2 unread → bell badge 2)
        s.notifications = [
            NotificationItem(id: "wei-n-nol", title: "Your nol balance is low",
                             body: "AED 14.50 left, about 4 metro trips.",
                             date: DemoDate.day(0, hour: 7, minute: 12), symbol: "tram.fill", hue: .sky,
                             isRead: false, link: nil),
            NotificationItem(id: "wei-n-du", title: "Your du bill is ready",
                             body: "Mobile: AED 125.00, due \(DemoDate.long(DemoDate.day(5))).",
                             date: DemoDate.day(-1, hour: 9, minute: 30), symbol: "phone.fill", hue: .sand,
                             isRead: false, link: nil),
            NotificationItem(id: "wei-n-bins", title: "Dubai Municipality has your report",
                             body: "A team will check Street 5 and answer by \(DemoDate.long(DemoDate.day(2))).",
                             date: DemoDate.day(-1, hour: 18, minute: 46), symbol: "exclamationmark.bubble.fill", hue: .mint,
                             isRead: true, link: .request("wei-bins")),
        ]

        s.linkedAccounts = [
            LinkedAccount(id: "rta", name: "RTA", detail: "nol card ending 4417", symbol: "tram.fill", hue: .sky, isLinked: true),
            LinkedAccount(id: "dewa", name: "DEWA", detail: "Electricity and water · 2061 0934", symbol: "bolt.fill", hue: .sand, isLinked: true),
            LinkedAccount(id: "du", name: "du", detail: "Mobile ending 6618", symbol: "phone.fill", hue: .sand, isLinked: true),
            LinkedAccount(id: "gdrfa", name: "GDRFA", detail: "Residence visa · valid to June 2027", symbol: "person.text.rectangle.fill", hue: .lilac, isLinked: true),
            LinkedAccount(id: "salik", name: "Salik", detail: "Not linked · no vehicle", symbol: "road.lanes", hue: .sand, isLinked: false),
            LinkedAccount(id: "eand", name: "e&", detail: "Not linked", symbol: "wifi", hue: .sand, isLinked: false),
        ]

        // MARK: Live things for widgets
        s.salikBalance = 0
        s.nolBalance = 14.50
        s.parking = nil
        s.energy = EnergyReading(monthLabel: "September", kWh: 612, changeVsLastMonth: -11,
                                 comparison: "Lower than 71% of similar homes in Al Nahda",
                                 lastSixMonths: [455, 538, 640, 702, 688, 612])
        s.metro = MetroStation(name: "Al Nahda", line: "Green Line", departures: [
            MetroDeparture(towards: "Creek", inMinutes: 4, nextInMinutes: 11),
            MetroDeparture(towards: "Etisalat by e&", inMinutes: 7, nextInMinutes: 15),
        ])
        s.weatherSpot = "Al Mamzar Beach"
        s.journeys = []
        return s
    }
}
