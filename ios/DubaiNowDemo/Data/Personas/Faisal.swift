import Foundation

// Faisal Rahman — café owner in Al Quoz 3 with two delivery vans (beat 2 of the "Four residents"
// video: Pay all clears the café's bills and a van's fine in one go). Numbers agree on every screen:
//   Needs you = 3 (2 due this week): café DEWA AED 1,486.20 due tomorrow (Account 2017 4420) ·
//   café du internet and phone AED 399.00 due in 3 days (Account 0045 2231) ·
//   traffic fine AED 150.00 on van 2 (Nissan Urvan G 20915, Al Karama, 5 days ago).
//   Pay all = 1,486.20 + 399.00 + 150.00 = AED 2,035.20 (all 3 payables).
//   Notifications: 5, 2 unread (café DEWA bill, the van fine). Salik: AED 245, 2 tags (topped up
//   AED 200 nine days ago → AED 311 then). Widgets: Bills and fines, Salik, café energy (medium),
//   prayer, weather.

extension Personas {
    static var faisal: Persona { Persona(
        id: "faisal",
        firstName: "Faisal",
        lastName: "Rahman",
        initials: "F",
        avatarHue: .sky,
        tagline: "Runs a café in Al Quoz with two delivery vans. Café bills and a van fine to clear in one go.",
        area: "Al Quoz 3",
        residency: "Residence visa",
        emiratesID: "784-1984-•••••••-7",
        phone: "+971 56 ••• 3075",
        assistantPrompts: [
            "Pay everything for the café",
            "Which van has a fine?",
            "How much power did the café use?",
            "Top up Salik with AED 100",
        ],
        makeState: { faisalState() }
    ) }

    static func faisalState() -> PersonaState {
        var s = PersonaState()

        let urvan = Plate(code: "G", number: "20915")

        // MARK: Payables (Pay all = AED 2,035.20)
        let dewa = Payable(
            id: "faisal-dewa-sep", kind: .bill, title: "Electricity & water bill", entity: "DEWA",
            detail: "Café · Account 2017 4420", amount: 1486.20, due: DemoDate.day(1),
            reference: "DEWA-2017-4420-0926", symbol: "bolt.fill", hue: .sand,
            serviceID: "pay-electricity-water-bill"
        )
        let du = Payable(
            id: "faisal-du-sep", kind: .bill, title: "Internet and phone bill", entity: "du",
            detail: "Café · Account 0045 2231", amount: 399.00, due: DemoDate.day(3),
            reference: "DU-0045-2231-0926", symbol: "wifi", hue: .sand,
            serviceID: "pay-du-bill"
        )
        let fine = Payable(
            id: "faisal-fine-van", kind: .fine, title: "Traffic fine", entity: "Dubai Police",
            detail: "Nissan Urvan G 20915 · Al Karama · \(DemoDate.short(DemoDate.day(-5)))", amount: 150, due: nil,
            reference: "DXB-TF-26-4471093", symbol: "exclamationmark.triangle.fill", hue: .sky,
            serviceID: "pay-traffic-fine", note: "On van 2"
        )

        // MARK: Needs you (by due date, the fine last)
        s.needsYou = [
            NeedsYouItem(
                id: "ny-dewa", kind: .payment(dewa), entity: "DEWA", symbol: "bolt.fill", hue: .sand,
                title: "Electricity & water bill", detail: "Café · Account 2017 4420",
                due: DemoDate.day(1), primaryAction: "Pay now", secondaryAction: "Later"
            ),
            NeedsYouItem(
                id: "ny-du", kind: .payment(du), entity: "du", symbol: "wifi", hue: .sand,
                title: "Internet and phone bill", detail: "Café · Account 0045 2231",
                due: DemoDate.day(3), primaryAction: "Pay now", secondaryAction: "Later"
            ),
            NeedsYouItem(
                id: "ny-fine", kind: .payment(fine), entity: "Dubai Police",
                symbol: "exclamationmark.triangle.fill", hue: .sky,
                title: "Traffic fine", detail: "Van 2 · Nissan Urvan G 20915",
                due: nil, primaryAction: "Pay now", secondaryAction: "Details"
            ),
        ]

        // MARK: Favorites and widgets
        s.favorites = [
            FavoriteItem(id: "fav-payall", title: "Pay all", symbol: "creditcard.fill", hue: .sky, target: .service("pay-all")),
            FavoriteItem(id: "fav-salik", title: "Salik", symbol: "road.lanes", hue: .sand, target: .service("top-up-salik-account")),
            FavoriteItem(id: "fav-vehicles", title: "Vehicles", symbol: "car.fill", hue: .rose, target: .thing("your-vehicles")),
            FavoriteItem(id: "fav-du", title: "Pay du", symbol: "phone.fill", hue: .sand, target: .service("pay-du-bill")),
            FavoriteItem(id: "fav-parking", title: "Pay parking", symbol: "p.square.fill", hue: .sky, target: .service("pay-for-parking")),
        ]
        s.widgets = [
            HomeWidget(id: "w-bills", kind: .billsAndFines, size: .small),
            HomeWidget(id: "w-salik", kind: .salik, size: .small),
            HomeWidget(id: "w-energy", kind: .energy, size: .medium),
            HomeWidget(id: "w-prayer", kind: .prayer, size: .small),
            HomeWidget(id: "w-weather", kind: .weather, size: .small),
        ]
        s.widgetSuggestion = nil

        // MARK: Things
        s.vehicles = [
            Vehicle(
                id: "faisal-van-1", make: "Toyota", model: "Hiace", year: 2021, colour: "White",
                plate: Plate(code: "F", number: "61742"), registrationExpiry: DemoDate.fixed(2027, 1, 20),
                insurer: "Orient Insurance", insuranceExpiry: DemoDate.fixed(2027, 1, 12),
                lastInspection: DemoDate.fixed(2026, 1, 14), salikTag: "Tag ending 2208"
            ),
            Vehicle(
                id: "faisal-van-2", make: "Nissan", model: "Urvan", year: 2022, colour: "White", plate: urvan,
                registrationExpiry: DemoDate.fixed(2027, 4, 8), insurer: "Orient Insurance",
                insuranceExpiry: DemoDate.fixed(2027, 4, 1), lastInspection: DemoDate.fixed(2026, 4, 2),
                salikTag: "Tag ending 6951", fineIDs: ["faisal-fine-van"]
            ),
        ]
        s.documents = [
            DocumentItem(id: "faisal-eid", title: "Emirates ID", number: "784-1984-•••••••-7", expiry: DemoDate.fixed(2027, 11, 3), symbol: "person.text.rectangle.fill", hue: .mint),
            DocumentItem(id: "faisal-visa-doc", title: "Residence visa", number: "201/2024/•••••87", expiry: DemoDate.fixed(2027, 11, 3), symbol: "person.text.rectangle", hue: .lilac),
            DocumentItem(id: "faisal-passport", title: "Passport", number: "India · Z••••4471", expiry: DemoDate.fixed(2032, 8, 15), symbol: "book.closed.fill", hue: .sand),
            DocumentItem(id: "faisal-tradelicence", title: "Trade licence", number: "Café · Licence ••••1186", expiry: DemoDate.fixed(2027, 2, 11), symbol: "storefront.fill", hue: .sand),
            DocumentItem(id: "faisal-licence", title: "Driving licence", number: "Licence ending 5582", expiry: DemoDate.fixed(2029, 5, 30), symbol: "car.fill", hue: .sky),
            DocumentItem(id: "faisal-ejari", title: "Tenancy contract", number: "Ejari · Al Quoz 3", expiry: DemoDate.fixed(2027, 2, 28), symbol: "key.fill", hue: .sand),
        ]
        s.family = [
            FamilyMember(id: "faisal-nadia", name: "Nadia Rahman", relation: "Wife", age: 34, visaExpiry: DemoDate.fixed(2027, 9, 14), sponsoredByYou: true, initials: "N", hue: .lilac),
            FamilyMember(id: "faisal-zayan", name: "Zayan Rahman", relation: "Son", age: 7, visaExpiry: DemoDate.fixed(2027, 9, 14), sponsoredByYou: true, initials: "Z", hue: .mint),
        ]
        // Both visas are fine for almost a year: nothing due.
        s.familyVisas = FamilyVisasStatus(memberIDs: ["faisal-nadia", "faisal-zayan"], dueMemberIDs: [], expiresOn: DemoDate.fixed(2027, 9, 14))
        s.home = HomeInfo(address: "Flat 304, Building 18, Street 6A, Al Quoz 3", dewaAccount: "2017 3895", ejariExpiry: DemoDate.fixed(2027, 2, 28), makani: "23054 71866")
        s.paymentMethods = [.applePay, .card(brand: "Visa", last4: "9014")]

        // MARK: Requests
        s.requests = [
            RequestItem(
                id: "faisal-waste", serviceID: "request-waste-container", title: "Get a bigger waste container for the café", entity: "Dubai Municipality",
                reference: "DM-WC-26-208814", status: .inReview(answerBy: DemoDate.day(3)),
                notice: nil,
                steps: [
                    RequestStep(title: "Ask for a bigger container", detail: "\(DemoDate.short(DemoDate.day(-2))) · 1,100-litre bin behind the café", state: .done),
                    RequestStep(title: "Dubai Municipality checks the site", detail: "Answer by \(DemoDate.short(DemoDate.day(3)))", state: .current),
                    RequestStep(title: "Your new container is delivered", detail: "Usually within 5 working days", state: .upcoming),
                ],
                startedOn: DemoDate.day(-2), symbol: "trash.fill", hue: .mint
            ),
            RequestItem(
                id: "faisal-power", serviceID: "report-power-water-cut", title: "Report a power cut at the café", entity: "DEWA",
                reference: "DEWA-OUT-26-55120", status: .done,
                notice: nil,
                steps: [
                    RequestStep(title: "Tell DEWA about the power cut", detail: "\(DemoDate.short(DemoDate.day(-6))), 07:40 · café, Al Quoz 3", state: .done),
                    RequestStep(title: "A DEWA team checks the supply", detail: "\(DemoDate.short(DemoDate.day(-6))), 08:15 · on site in 35 minutes", state: .done),
                    RequestStep(title: "Power is back on", detail: "\(DemoDate.short(DemoDate.day(-6))), 09:05", state: .done),
                ],
                startedOn: DemoDate.day(-6), symbol: "bolt.fill", hue: .sand
            ),
        ]

        // MARK: Drafts (kept 30 days)
        s.drafts = [
            DraftItem(id: "faisal-draft-parking", serviceID: "buy-parking-subscription", title: "Buy a parking subscription", stepsDone: 1, stepsTotal: 3, savedOn: DemoDate.day(-5), symbol: "p.square.fill", hue: .sky),
        ]

        // MARK: History (newest first)
        s.history = [
            Receipt(id: "faisal-r-parking", title: "Paid for parking", shortTitle: "Paid for parking", entity: "Parkin", amount: 52,
                    date: DemoDate.day(-3, hour: 17, minute: 48), method: .applePay, reference: "PKN-26-80455",
                    rows: [ReceiptRow(label: "Sessions", value: "7 in Business Bay and Al Karama")], symbol: "p.square.fill", hue: .sky,
                    serviceID: "pay-for-parking", count: 7),
            Receipt(id: "faisal-r-salik", title: "Topped up your Salik account", shortTitle: "Topped up Salik", entity: "Salik", amount: 200,
                    date: DemoDate.day(-9, hour: 7, minute: 52), method: .applePay, reference: "SLK-26-8106",
                    rows: [ReceiptRow(label: "New balance", value: "AED 311.00")], symbol: "road.lanes", hue: .sand,
                    serviceID: "top-up-salik-account"),
            Receipt(id: "faisal-r-du", title: "Paid your du bill", shortTitle: "Paid du", entity: "du", amount: 399.00,
                    date: DemoDate.day(-27, hour: 22, minute: 10), method: .card(brand: "Visa", last4: "9014"), reference: "DU-0045-2231-0826",
                    rows: [ReceiptRow(label: "For", value: "August · café internet and phone")], symbol: "wifi", hue: .sand,
                    serviceID: "pay-du-bill"),
            Receipt(id: "faisal-r-dewa", title: "Paid your electricity and water bill", shortTitle: "Paid electricity and water", entity: "DEWA", amount: 1612.40,
                    date: DemoDate.day(-30, hour: 22, minute: 4), method: .card(brand: "Visa", last4: "9014"), reference: "DEWA-2017-4420-0826",
                    rows: [ReceiptRow(label: "For", value: "August · café, Al Quoz 3")], symbol: "bolt.fill", hue: .sand,
                    serviceID: "pay-electricity-water-bill", rating: .happy),
        ]

        // MARK: Notifications (2 unread → bell badge 2)
        s.notifications = [
            NotificationItem(id: "faisal-n-dewa", title: "The café's September bill is ready",
                             body: "Electricity and water: AED 1,486.20, due tomorrow.",
                             date: DemoDate.day(-2, hour: 10, minute: 15), symbol: "bolt.fill", hue: .sand,
                             isRead: false, link: nil),
            NotificationItem(id: "faisal-n-fine", title: "New fine on van 2",
                             body: "Traffic fine: AED 150 on your Nissan Urvan, G 20915, in Al Karama.",
                             date: DemoDate.day(-5, hour: 13, minute: 20), symbol: "exclamationmark.triangle.fill", hue: .sky,
                             isRead: false, link: .vehicle("faisal-van-2")),
            NotificationItem(id: "faisal-n-waste", title: "Dubai Municipality has your request",
                             body: "They'll check the site behind the café and answer by \(DemoDate.long(DemoDate.day(3))).",
                             date: DemoDate.day(-2, hour: 11, minute: 2), symbol: "trash.fill", hue: .mint,
                             isRead: true, link: .request("faisal-waste")),
            NotificationItem(id: "faisal-n-du", title: "Your du bill is ready",
                             body: "Café internet and phone: AED 399.00, due \(DemoDate.long(DemoDate.day(3))).",
                             date: DemoDate.day(-4, hour: 9, minute: 0), symbol: "wifi", hue: .sand,
                             isRead: true, link: nil),
            NotificationItem(id: "faisal-n-power", title: "Power is back on at the café",
                             body: "DEWA fixed the supply at 09:05. Thanks for reporting it.",
                             date: DemoDate.day(-6, hour: 9, minute: 6), symbol: "bolt.fill", hue: .sand,
                             isRead: true, link: .request("faisal-power")),
        ]

        s.linkedAccounts = [
            LinkedAccount(id: "dewa", name: "DEWA", detail: "2 accounts · home and café", symbol: "bolt.fill", hue: .sand, isLinked: true),
            LinkedAccount(id: "rta", name: "RTA", detail: "2 vans and your driving licence", symbol: "car.fill", hue: .sky, isLinked: true),
            LinkedAccount(id: "police", name: "Dubai Police", detail: "Traffic fines", symbol: "shield.fill", hue: .rose, isLinked: true),
            LinkedAccount(id: "salik", name: "Salik", detail: "2 tags", symbol: "road.lanes", hue: .sand, isLinked: true),
            LinkedAccount(id: "parkin", name: "Parkin", detail: "Parking for 2 vans", symbol: "p.square.fill", hue: .sky, isLinked: true),
            LinkedAccount(id: "du", name: "du", detail: "Café internet and phone · 0045 2231", symbol: "wifi", hue: .sand, isLinked: true),
            LinkedAccount(id: "eand", name: "e&", detail: "Mobile ending 3075", symbol: "phone.fill", hue: .sand, isLinked: true),
        ]

        // MARK: Live things for widgets
        s.salikBalance = 245
        s.nolBalance = nil
        s.parking = nil
        // The café's DEWA account (2017 4420).
        s.energy = EnergyReading(monthLabel: "September", kWh: 3412, changeVsLastMonth: -7,
                                 comparison: "Lower than 54% of similar cafés in Al Quoz",
                                 lastSixMonths: [2610, 2980, 3390, 3720, 3655, 3412])
        s.metro = nil
        s.journeys = []
        return s
    }
}
