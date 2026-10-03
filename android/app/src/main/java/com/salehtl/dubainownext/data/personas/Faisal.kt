package com.salehtl.dubainownext.data.personas

import com.salehtl.dubainownext.app.Route
import com.salehtl.dubainownext.data.DemoDate
import com.salehtl.dubainownext.data.DocumentItem
import com.salehtl.dubainownext.data.DraftItem
import com.salehtl.dubainownext.data.EnergyReading
import com.salehtl.dubainownext.data.FamilyMember
import com.salehtl.dubainownext.data.FamilyVisasStatus
import com.salehtl.dubainownext.data.FavoriteItem
import com.salehtl.dubainownext.data.HomeInfo
import com.salehtl.dubainownext.data.HomeWidget
import com.salehtl.dubainownext.data.LinkedAccount
import com.salehtl.dubainownext.data.NeedsYouItem
import com.salehtl.dubainownext.data.NotificationItem
import com.salehtl.dubainownext.data.Payable
import com.salehtl.dubainownext.data.PaymentMethod
import com.salehtl.dubainownext.data.Persona
import com.salehtl.dubainownext.data.PersonaState
import com.salehtl.dubainownext.data.Plate
import com.salehtl.dubainownext.data.Receipt
import com.salehtl.dubainownext.data.ReceiptRow
import com.salehtl.dubainownext.data.RequestItem
import com.salehtl.dubainownext.data.RequestStep
import com.salehtl.dubainownext.data.Vehicle
import com.salehtl.dubainownext.data.WidgetKind
import com.salehtl.dubainownext.data.WidgetSize
import com.salehtl.dubainownext.data.aed
import com.salehtl.dubainownext.design.Hue
import com.salehtl.dubainownext.design.components.HappinessFace

// Faisal Rahman — café owner in Al Quoz 3 with two delivery vans (beat 2 of the "Four residents"
// video: Pay all clears the café's bills and a van's fine in one go). Ported from iOS Faisal.swift; same numbers:
//   Needs you = 3 (2 due this week): café DEWA AED 1,486.20 due tomorrow (Account 2017 4420) ·
//   café du internet and phone AED 399.00 due in 3 days (Account 0045 2231) ·
//   traffic fine AED 150.00 on van 2 (Nissan Urvan G 20915, Al Karama, 5 days ago).
//   Pay all = 1,486.20 + 399.00 + 150.00 = AED 2,035.20 (all 3 payables).
//   Notifications: 5, 2 unread (café DEWA bill, the van fine). Salik: AED 245, 2 tags (topped up
//   AED 200 nine days ago → AED 311 then). Widgets: Bills and fines, Salik, café energy (medium),
//   prayer, weather.

val faisal: Persona
    get() = Persona(
        id = "faisal", firstName = "Faisal", lastName = "Rahman", initials = "F", avatarHue = Hue.Sky,
        tagline = "Runs a café in Al Quoz with two delivery vans. Café bills and a van fine to clear in one go.",
        area = "Al Quoz 3", residency = "Residence visa", emiratesID = "784-1984-•••••••-7", phone = "+971 56 ••• 3075",
        assistantPrompts = listOf("Pay everything for the café", "Which van has a fine?", "How much power did the café use?", "Top up Salik with AED 100"),
        makeState = { faisalState() },
    )

fun faisalState(): PersonaState {
    val urvan = Plate(code = "G", number = "20915")

    // Payables (Pay all = AED 2,035.20)
    val dewa = Payable(
        id = "faisal-dewa-sep", kind = Payable.Kind.Bill, title = "Electricity & water bill", entity = "DEWA",
        detail = "Café · Account 2017 4420", amount = 1486.20.aed, due = DemoDate.day(1),
        reference = "DEWA-2017-4420-0926", symbol = "bolt.fill", hue = Hue.Sand, serviceID = "pay-electricity-water-bill",
    )
    val du = Payable(
        id = "faisal-du-sep", kind = Payable.Kind.Bill, title = "Internet and phone bill", entity = "du",
        detail = "Café · Account 0045 2231", amount = 399.00.aed, due = DemoDate.day(3),
        reference = "DU-0045-2231-0926", symbol = "wifi", hue = Hue.Sand, serviceID = "pay-du-bill",
    )
    val fine = Payable(
        id = "faisal-fine-van", kind = Payable.Kind.Fine, title = "Traffic fine", entity = "Dubai Police",
        detail = "Nissan Urvan G 20915 · Al Karama · ${DemoDate.short(DemoDate.day(-5))}", amount = 150.aed, due = null,
        reference = "DXB-TF-26-4471093", symbol = "exclamationmark.triangle.fill", hue = Hue.Sky,
        serviceID = "pay-traffic-fine", note = "On van 2",
    )
    return PersonaState(
        // By due date, the fine last.
        needsYou = listOf(
            NeedsYouItem("ny-dewa", NeedsYouItem.Kind.Payment(dewa), "DEWA", "bolt.fill", Hue.Sand,
                "Electricity & water bill", "Café · Account 2017 4420", DemoDate.day(1), "Pay now", "Later"),
            NeedsYouItem("ny-du", NeedsYouItem.Kind.Payment(du), "du", "wifi", Hue.Sand,
                "Internet and phone bill", "Café · Account 0045 2231", DemoDate.day(3), "Pay now", "Later"),
            NeedsYouItem("ny-fine", NeedsYouItem.Kind.Payment(fine), "Dubai Police", "exclamationmark.triangle.fill", Hue.Sky,
                "Traffic fine", "Van 2 · Nissan Urvan G 20915", null, "Pay now", "Details"),
        ),
        favorites = listOf(
            FavoriteItem("fav-payall", "Pay all", "creditcard.fill", Hue.Sky, FavoriteItem.Target.Service("pay-all")),
            FavoriteItem("fav-salik", "Salik", "road.lanes", Hue.Sand, FavoriteItem.Target.Service("top-up-salik-account")),
            FavoriteItem("fav-vehicles", "Vehicles", "car.fill", Hue.Rose, FavoriteItem.Target.Thing("your-vehicles")),
            FavoriteItem("fav-du", "Pay du", "phone.fill", Hue.Sand, FavoriteItem.Target.Service("pay-du-bill")),
            FavoriteItem("fav-parking", "Pay parking", "p.square.fill", Hue.Sky, FavoriteItem.Target.Service("pay-for-parking")),
        ),
        widgets = listOf(
            HomeWidget("w-bills", WidgetKind.BillsAndFines, WidgetSize.Small),
            HomeWidget("w-salik", WidgetKind.Salik, WidgetSize.Small),
            HomeWidget("w-energy", WidgetKind.Energy, WidgetSize.Medium),
            HomeWidget("w-prayer", WidgetKind.Prayer, WidgetSize.Small),
            HomeWidget("w-weather", WidgetKind.Weather, WidgetSize.Small),
        ),
        widgetSuggestion = null,
        vehicles = listOf(
            Vehicle("faisal-van-1", "Toyota", "Hiace", 2021, "White", Plate(code = "F", number = "61742"), DemoDate.fixed(2027, 1, 20),
                "Orient Insurance", DemoDate.fixed(2027, 1, 12), DemoDate.fixed(2026, 1, 14), "Tag ending 2208"),
            Vehicle("faisal-van-2", "Nissan", "Urvan", 2022, "White", urvan, DemoDate.fixed(2027, 4, 8), "Orient Insurance",
                DemoDate.fixed(2027, 4, 1), DemoDate.fixed(2026, 4, 2), "Tag ending 6951", listOf("faisal-fine-van")),
        ),
        documents = listOf(
            DocumentItem("faisal-eid", "Emirates ID", "784-1984-•••••••-7", DemoDate.fixed(2027, 11, 3), symbol = "person.text.rectangle.fill", hue = Hue.Mint),
            DocumentItem("faisal-visa-doc", "Residence visa", "201/2024/•••••87", DemoDate.fixed(2027, 11, 3), symbol = "person.text.rectangle", hue = Hue.Lilac),
            DocumentItem("faisal-passport", "Passport", "India · Z••••4471", DemoDate.fixed(2032, 8, 15), symbol = "book.closed.fill", hue = Hue.Sand),
            DocumentItem("faisal-tradelicence", "Trade licence", "Café · Licence ••••1186", DemoDate.fixed(2027, 2, 11), symbol = "storefront.fill", hue = Hue.Sand),
            DocumentItem("faisal-licence", "Driving licence", "Licence ending 5582", DemoDate.fixed(2029, 5, 30), symbol = "car.fill", hue = Hue.Sky),
            DocumentItem("faisal-ejari", "Tenancy contract", "Ejari · Al Quoz 3", DemoDate.fixed(2027, 2, 28), symbol = "key.fill", hue = Hue.Sand),
        ),
        family = listOf(
            FamilyMember("faisal-nadia", "Nadia Rahman", "Wife", 34, DemoDate.fixed(2027, 9, 14), true, "N", Hue.Lilac),
            FamilyMember("faisal-zayan", "Zayan Rahman", "Son", 7, DemoDate.fixed(2027, 9, 14), true, "Z", Hue.Mint),
        ),
        // Both visas are fine for almost a year: nothing due.
        familyVisas = FamilyVisasStatus(listOf("faisal-nadia", "faisal-zayan"), emptyList(), DemoDate.fixed(2027, 9, 14)),
        home = HomeInfo("Flat 304, Building 18, Street 6A, Al Quoz 3", "2017 3895", DemoDate.fixed(2027, 2, 28), "23054 71866"),
        paymentMethods = listOf(PaymentMethod.GooglePay, PaymentMethod.Card("Visa", "9014")),
        requests = listOf(
            RequestItem(
                "faisal-waste", "request-waste-container", "Get a bigger waste container for the café", "Dubai Municipality", "DM-WC-26-208814",
                RequestItem.Status.InReview(DemoDate.day(3)), null,
                listOf(
                    RequestStep("Ask for a bigger container", "${DemoDate.short(DemoDate.day(-2))} · 1,100-litre bin behind the café", RequestStep.State.Done),
                    RequestStep("Dubai Municipality checks the site", "Answer by ${DemoDate.short(DemoDate.day(3))}", RequestStep.State.Current),
                    RequestStep("Your new container is delivered", "Usually within 5 working days", RequestStep.State.Upcoming),
                ),
                DemoDate.day(-2), "trash.fill", Hue.Mint,
            ),
            RequestItem(
                "faisal-power", "report-power-water-cut", "Report a power cut at the café", "DEWA", "DEWA-OUT-26-55120",
                RequestItem.Status.Done, null,
                listOf(
                    RequestStep("Tell DEWA about the power cut", "${DemoDate.short(DemoDate.day(-6))}, 07:40 · café, Al Quoz 3", RequestStep.State.Done),
                    RequestStep("A DEWA team checks the supply", "${DemoDate.short(DemoDate.day(-6))}, 08:15 · on site in 35 minutes", RequestStep.State.Done),
                    RequestStep("Power is back on", "${DemoDate.short(DemoDate.day(-6))}, 09:05", RequestStep.State.Done),
                ),
                DemoDate.day(-6), "bolt.fill", Hue.Sand,
            ),
        ),
        drafts = listOf(
            DraftItem("faisal-draft-parking", "buy-parking-subscription", "Buy a parking subscription", 1, 3, DemoDate.day(-5), "p.square.fill", Hue.Sky),
        ),
        history = listOf(
            Receipt("faisal-r-parking", "Paid for parking", "Paid for parking", "Parkin", 52.aed, DemoDate.day(-3, 17, 48), PaymentMethod.GooglePay,
                "PKN-26-80455", listOf(ReceiptRow("Sessions", "7 in Business Bay and Al Karama")), "p.square.fill", Hue.Sky, "pay-for-parking", count = 7),
            Receipt("faisal-r-salik", "Topped up your Salik account", "Topped up Salik", "Salik", 200.aed, DemoDate.day(-9, 7, 52), PaymentMethod.GooglePay,
                "SLK-26-8106", listOf(ReceiptRow("New balance", "AED 311.00")), "road.lanes", Hue.Sand, "top-up-salik-account"),
            Receipt("faisal-r-du", "Paid your du bill", "Paid du", "du", 399.00.aed, DemoDate.day(-27, 22, 10), PaymentMethod.Card("Visa", "9014"),
                "DU-0045-2231-0826", listOf(ReceiptRow("For", "August · café internet and phone")), "wifi", Hue.Sand, "pay-du-bill"),
            Receipt("faisal-r-dewa", "Paid your electricity and water bill", "Paid electricity and water", "DEWA", 1612.40.aed, DemoDate.day(-30, 22, 4),
                PaymentMethod.Card("Visa", "9014"), "DEWA-2017-4420-0826", listOf(ReceiptRow("For", "August · café, Al Quoz 3")), "bolt.fill", Hue.Sand,
                "pay-electricity-water-bill", rating = HappinessFace.Happy),
        ),
        // 2 unread → bell badge 2.
        notifications = listOf(
            NotificationItem("faisal-n-dewa", "The café's September bill is ready", "Electricity and water: AED 1,486.20, due tomorrow.",
                DemoDate.day(-2, 10, 15), "bolt.fill", Hue.Sand, false, null),
            NotificationItem("faisal-n-fine", "New fine on van 2", "Traffic fine: AED 150 on your Nissan Urvan, G 20915, in Al Karama.",
                DemoDate.day(-5, 13, 20), "exclamationmark.triangle.fill", Hue.Sky, false, Route.Vehicle("faisal-van-2")),
            NotificationItem("faisal-n-waste", "Dubai Municipality has your request",
                "They'll check the site behind the café and answer by ${DemoDate.long(DemoDate.day(3))}.",
                DemoDate.day(-2, 11, 2), "trash.fill", Hue.Mint, true, Route.Request("faisal-waste")),
            NotificationItem("faisal-n-du", "Your du bill is ready", "Café internet and phone: AED 399.00, due ${DemoDate.long(DemoDate.day(3))}.",
                DemoDate.day(-4, 9, 0), "wifi", Hue.Sand, true, null),
            NotificationItem("faisal-n-power", "Power is back on at the café", "DEWA fixed the supply at 09:05. Thanks for reporting it.",
                DemoDate.day(-6, 9, 6), "bolt.fill", Hue.Sand, true, Route.Request("faisal-power")),
        ),
        linkedAccounts = listOf(
            LinkedAccount("dewa", "DEWA", "2 accounts · home and café", "bolt.fill", Hue.Sand, true),
            LinkedAccount("rta", "RTA", "2 vans and your driving licence", "car.fill", Hue.Sky, true),
            LinkedAccount("police", "Dubai Police", "Traffic fines", "shield.fill", Hue.Rose, true),
            LinkedAccount("salik", "Salik", "2 tags", "road.lanes", Hue.Sand, true),
            LinkedAccount("parkin", "Parkin", "Parking for 2 vans", "p.square.fill", Hue.Sky, true),
            LinkedAccount("du", "du", "Café internet and phone · 0045 2231", "wifi", Hue.Sand, true),
            LinkedAccount("eand", "e&", "Mobile ending 3075", "phone.fill", Hue.Sand, true),
        ),
        salikBalance = 245.aed,
        nolBalance = null,
        parking = null,
        // The café's DEWA account (2017 4420).
        energy = EnergyReading("September", 3412, -7, "Lower than 54% of similar cafés in Al Quoz", listOf(2610, 2980, 3390, 3720, 3655, 3412)),
        metro = null,
        journeys = emptyList(),
    )
}
