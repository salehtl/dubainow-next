package com.salehtl.dubainownext.data.personas

import com.salehtl.dubainownext.app.Route
import com.salehtl.dubainownext.data.DemoDate
import com.salehtl.dubainownext.data.DocumentItem
import com.salehtl.dubainownext.data.DraftItem
import com.salehtl.dubainownext.data.EnergyReading
import com.salehtl.dubainownext.data.FavoriteItem
import com.salehtl.dubainownext.data.HomeInfo
import com.salehtl.dubainownext.data.HomeWidget
import com.salehtl.dubainownext.data.LinkedAccount
import com.salehtl.dubainownext.data.MetroDeparture
import com.salehtl.dubainownext.data.MetroStation
import com.salehtl.dubainownext.data.NeedsYouItem
import com.salehtl.dubainownext.data.NotificationItem
import com.salehtl.dubainownext.data.Payable
import com.salehtl.dubainownext.data.PaymentMethod
import com.salehtl.dubainownext.data.Persona
import com.salehtl.dubainownext.data.PersonaState
import com.salehtl.dubainownext.data.Receipt
import com.salehtl.dubainownext.data.ReceiptRow
import com.salehtl.dubainownext.data.RequestItem
import com.salehtl.dubainownext.data.RequestStep
import com.salehtl.dubainownext.data.WidgetKind
import com.salehtl.dubainownext.data.WidgetSize
import com.salehtl.dubainownext.data.aed
import com.salehtl.dubainownext.design.Hue
import com.salehtl.dubainownext.design.components.HappinessFace

// Wei Zhang — site engineer from Chengdu living in Al Nahda 2, on a residence visa through his
// employer (Crestline Contracting LLC). No car: he takes the Green Line from Al Nahda and uses a
// nol card. Assistant showcase: he asks in Mandarin about two months of unpaid salary and is led to
// "Report a problem with your employer" (report-employer-problem). Ported from iOS Wei.swift; same numbers:
//   Needs you = 1 (1 due this week): du mobile bill AED 125.00 due in 5 days (mobile ending 6618).
//   Pay all doesn't show (only 1 payable); the Pay all bundle total would be AED 125.00.
//   nol: AED 14.50 (low, about 4 trips; topped up AED 50 eighteen days ago → AED 61.50 then).
//   Notifications: 3, 2 unread (du bill, low nol). Salik: none (no car).
//   Widgets: nol, weather (Al Mamzar Beach), metro (Al Nahda, Green Line, medium), energy (medium).

val wei: Persona
    get() = Persona(
        id = "wei", firstName = "Wei", lastName = "Zhang", initials = "W", avatarHue = Hue.Mint,
        tagline = "Site engineer from Chengdu in Al Nahda. Takes the metro, asks the assistant in Mandarin.",
        area = "Al Nahda 2", residency = "Residence visa", emiratesID = "784-1993-•••••••-4", phone = "+971 52 ••• 6618",
        assistantPrompts = listOf("我的工资两个月没发了，怎么办？", "Top up my nol card with AED 50", "Check my visa status", "When is the next metro?"),
        makeState = { weiState() },
    )

fun weiState(): PersonaState {
    val du = Payable(
        id = "wei-du-sep", kind = Payable.Kind.Bill, title = "Mobile bill", entity = "du",
        detail = "Mobile ending 6618", amount = 125.00.aed, due = DemoDate.day(5),
        reference = "DU-M-6618-0926", symbol = "phone.fill", hue = Hue.Sand, serviceID = "pay-du-bill",
    )
    return PersonaState(
        needsYou = listOf(
            NeedsYouItem("ny-du", NeedsYouItem.Kind.Payment(du), "du", "phone.fill", Hue.Sand,
                "Mobile bill", "Mobile ending 6618", DemoDate.day(5), "Pay now", "Later"),
        ),
        favorites = listOf(
            FavoriteItem("fav-nol", "Top up nol", "tram.fill", Hue.Sky, FavoriteItem.Target.Service("top-up-nol-card")),
            FavoriteItem("fav-visa", "Visa status", "person.text.rectangle.fill", Hue.Lilac, FavoriteItem.Target.Service("check-visa-status")),
            FavoriteItem("fav-employer", "Worker's Voice", "exclamationmark.bubble.fill", Hue.Sand, FavoriteItem.Target.Service("report-employer-problem")),
            FavoriteItem("fav-du", "Pay du", "phone.fill", Hue.Sand, FavoriteItem.Target.Service("pay-du-bill")),
        ),
        widgets = listOf(
            HomeWidget("w-nol", WidgetKind.Nol, WidgetSize.Small),
            HomeWidget("w-weather", WidgetKind.Weather, WidgetSize.Small),
            HomeWidget("w-metro", WidgetKind.Metro, WidgetSize.Medium),
            HomeWidget("w-energy", WidgetKind.Energy, WidgetSize.Medium),
        ),
        widgetSuggestion = null,
        // No vehicles.
        vehicles = emptyList(),
        documents = listOf(
            DocumentItem("wei-eid", "Emirates ID", "784-1993-•••••••-4", DemoDate.fixed(2027, 6, 21), symbol = "person.text.rectangle.fill", hue = Hue.Mint),
            DocumentItem("wei-visa-doc", "Residence visa", "201/2025/•••••19", DemoDate.fixed(2027, 6, 21), symbol = "person.text.rectangle", hue = Hue.Lilac),
            DocumentItem("wei-passport", "Passport", "China · E••••8126", DemoDate.fixed(2033, 4, 2), symbol = "book.closed.fill", hue = Hue.Sand),
            DocumentItem("wei-workpermit", "Work permit", "Crestline Contracting · ••••7720", DemoDate.fixed(2027, 6, 21), symbol = "briefcase.fill", hue = Hue.Sand),
            DocumentItem("wei-insurance", "Health insurance card", "Orient Insurance · ••••3391", DemoDate.fixed(2027, 6, 21), symbol = "cross.case.fill", hue = Hue.Mint),
        ),
        // His wife and daughter live in Chengdu; nobody in Dubai is on his visa.
        family = emptyList(),
        familyVisas = null,
        home = HomeInfo("Flat 506, Building 7, Al Nahda 2", "2061 0934", DemoDate.fixed(2027, 3, 31), "33718 92045"),
        paymentMethods = listOf(PaymentMethod.GooglePay, PaymentMethod.Card("Mastercard", "3306")),
        requests = listOf(
            RequestItem(
                "wei-bins", "report-city-problem", "Report overflowing bins on your street", "Dubai Municipality", "DM-RP-26-771204",
                RequestItem.Status.InReview(DemoDate.day(2)), null,
                listOf(
                    RequestStep("Tell Dubai Municipality what's wrong", "${DemoDate.short(DemoDate.day(-1))} · Street 5, Al Nahda 2, with a photo", RequestStep.State.Done),
                    RequestStep("A team checks the street", "Answer by ${DemoDate.short(DemoDate.day(2))}", RequestStep.State.Current),
                    RequestStep("The bins are emptied", "We'll let you know", RequestStep.State.Upcoming),
                ),
                DemoDate.day(-1), "exclamationmark.bubble.fill", Hue.Mint,
            ),
        ),
        drafts = listOf(
            DraftItem("wei-draft-beach", "book-park-visit", "Book a park or beach visit", 1, 2, DemoDate.day(-3), "tree.fill", Hue.Mint),
        ),
        history = listOf(
            Receipt("wei-r-dewa", "Paid your electricity and water bill", "Paid electricity and water", "DEWA", 341.90.aed, DemoDate.day(-2, 21, 37),
                PaymentMethod.GooglePay, "DEWA-2061-0934-0926", listOf(ReceiptRow("For", "September · Al Nahda 2")), "bolt.fill", Hue.Sand,
                "pay-electricity-water-bill", rating = HappinessFace.Happy),
            Receipt("wei-r-nol", "Topped up your nol card", "Topped up nol", "RTA", 50.aed, DemoDate.day(-18, 7, 21), PaymentMethod.GooglePay,
                "NOL-26-339170", listOf(ReceiptRow("New balance", "AED 61.50")), "tram.fill", Hue.Sky, "top-up-nol-card"),
            Receipt("wei-r-du", "Paid your du bill", "Paid du", "du", 125.00.aed, DemoDate.day(-30, 20, 44), PaymentMethod.GooglePay,
                "DU-M-6618-0826", listOf(ReceiptRow("For", "August · mobile ending 6618")), "phone.fill", Hue.Sand, "pay-du-bill"),
            Receipt("wei-r-nol-aug", "Topped up your nol card", "Topped up nol", "RTA", 50.aed, DemoDate.day(-41, 7, 18), PaymentMethod.GooglePay,
                "NOL-26-301884", listOf(ReceiptRow("New balance", "AED 58.00")), "tram.fill", Hue.Sky, "top-up-nol-card"),
        ),
        // 2 unread → bell badge 2.
        notifications = listOf(
            NotificationItem("wei-n-nol", "Your nol balance is low", "AED 14.50 left, about 4 metro trips.",
                DemoDate.day(0, 7, 12), "tram.fill", Hue.Sky, false, null),
            NotificationItem("wei-n-du", "Your du bill is ready", "Mobile: AED 125.00, due ${DemoDate.long(DemoDate.day(5))}.",
                DemoDate.day(-1, 9, 30), "phone.fill", Hue.Sand, false, null),
            NotificationItem("wei-n-bins", "Dubai Municipality has your report", "A team will check Street 5 and answer by ${DemoDate.long(DemoDate.day(2))}.",
                DemoDate.day(-1, 18, 46), "exclamationmark.bubble.fill", Hue.Mint, true, Route.Request("wei-bins")),
        ),
        linkedAccounts = listOf(
            LinkedAccount("rta", "RTA", "nol card ending 4417", "tram.fill", Hue.Sky, true),
            LinkedAccount("dewa", "DEWA", "Electricity and water · 2061 0934", "bolt.fill", Hue.Sand, true),
            LinkedAccount("du", "du", "Mobile ending 6618", "phone.fill", Hue.Sand, true),
            LinkedAccount("gdrfa", "GDRFA", "Residence visa · valid to June 2027", "person.text.rectangle.fill", Hue.Lilac, true),
            LinkedAccount("salik", "Salik", "Not linked · no vehicle", "road.lanes", Hue.Sand, false),
            LinkedAccount("eand", "e&", "Not linked", "wifi", Hue.Sand, false),
        ),
        salikBalance = 0.aed,
        nolBalance = 14.50.aed,
        parking = null,
        energy = EnergyReading("September", 612, -11, "Lower than 71% of similar homes in Al Nahda", listOf(455, 538, 640, 702, 688, 612)),
        metro = MetroStation("Al Nahda", "Green Line", listOf(MetroDeparture("Creek", 4, 11), MetroDeparture("Etisalat by e&", 7, 15))),
        weatherSpot = "Al Mamzar Beach",
        journeys = emptyList(),
    )
}
