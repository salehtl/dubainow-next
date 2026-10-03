package com.salehtl.dubainownext.data.personas

import com.salehtl.dubainownext.app.Route
import com.salehtl.dubainownext.data.DemoDate
import com.salehtl.dubainownext.data.DocumentItem
import com.salehtl.dubainownext.data.DraftItem
import com.salehtl.dubainownext.data.EnergyReading
import com.salehtl.dubainownext.data.FamilyMember
import com.salehtl.dubainownext.data.FavoriteItem
import com.salehtl.dubainownext.data.HomeInfo
import com.salehtl.dubainownext.data.HomeWidget
import com.salehtl.dubainownext.data.JourneyProgress
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
import com.salehtl.dubainownext.data.StepProgress
import com.salehtl.dubainownext.data.Vehicle
import com.salehtl.dubainownext.data.WidgetKind
import com.salehtl.dubainownext.data.WidgetSize
import com.salehtl.dubainownext.data.aed
import com.salehtl.dubainownext.design.Hue
import com.salehtl.dubainownext.design.components.HappinessFace

// Khalid Al Mansoori — Emirati father building the family house (Paper C1 "Home · A journey on
// Home (Khalid)", and beat 1 of the "Four residents" video). Ported from iOS Khalid.swift; same numbers:
//   Lives in Al Mizhar 1, building on his plot in Al Khawaneej 2.
//   Build a house: month 9, stage 4 of 7 "Design" — Plan done in January, Land in April,
//   Money in July (MBRHE housing loan approved 16 July). Your turn: "Review your villa drawings",
//   sent by his consultant today. Next: Permits (1 to 2 months).
//   Needs you = 2 (1 due this week): the drawings review (journey step, no due date) ·
//   DEWA AED 386.20 due in 5 days (Al Mizhar 1 · Account 2003 5521).
//   Pay all doesn't show (only 1 payable); the Pay all bundle total would be AED 386.20.
//   Notifications: 4, 2 unread (drawings today, DEWA bill). Salik: AED 96 (topped up AED 100
//   22 days ago → AED 146 then). Widgets: Build a house (large), prayer, weather, energy (medium).

val khalid: Persona
    get() = Persona(
        id = "khalid", firstName = "Khalid", lastName = "Al Mansoori", initials = "K", avatarHue = Hue.Sand,
        tagline = "Emirati father building the family house in Al Khawaneej 2. Villa drawings to review.",
        area = "Al Mizhar 1", residency = "UAE national", emiratesID = "784-1985-•••••••-1", phone = "+971 55 ••• 1200",
        assistantPrompts = listOf("What's next for my house?", "Show me my villa drawings", "How much do I owe?", "Top up Salik with AED 100"),
        makeState = { khalidState() },
    )

fun khalidState(): PersonaState {
    val dewa = Payable(
        id = "khalid-dewa-sep", kind = Payable.Kind.Bill, title = "Electricity & water bill", entity = "DEWA",
        detail = "Al Mizhar 1 · Account 2003 5521", amount = 386.20.aed, due = DemoDate.day(5),
        reference = "DEWA-2003-5521-0926", symbol = "bolt.fill", hue = Hue.Sand, serviceID = "pay-electricity-water-bill",
    )
    val cruiser = Plate(code = "L", number = "7340")
    return PersonaState(
        // The journey step leads, as on Paper.
        needsYou = listOf(
            NeedsYouItem("ny-house", NeedsYouItem.Kind.JourneyStep("build-a-house"), "Build a house", "house.lodge.fill", Hue.Sand,
                "Review your villa drawings", "Sent by your consultant today", null, "Review drawings", null,
                progress = StepProgress(4, 7)),
            NeedsYouItem("ny-dewa", NeedsYouItem.Kind.Payment(dewa), "DEWA", "bolt.fill", Hue.Sand,
                "Electricity & water bill", "Al Mizhar 1 · Account 2003 5521", DemoDate.day(5), "Pay now", "Later"),
        ),
        favorites = listOf(
            FavoriteItem("fav-payall", "Pay all", "creditcard.fill", Hue.Sky, FavoriteItem.Target.Service("pay-all")),
            FavoriteItem("fav-salik", "Salik", "road.lanes", Hue.Sand, FavoriteItem.Target.Service("top-up-salik-account")),
            FavoriteItem("fav-vehicles", "Vehicles", "car.fill", Hue.Rose, FavoriteItem.Target.Thing("your-vehicles")),
            FavoriteItem("fav-donate", "Donate", "heart.fill", Hue.Lilac, FavoriteItem.Target.Service("donate-to-charity")),
        ),
        widgets = listOf(
            HomeWidget("w-house", WidgetKind.Journey("build-a-house"), WidgetSize.Large),
            HomeWidget("w-prayer", WidgetKind.Prayer, WidgetSize.Small),
            HomeWidget("w-weather", WidgetKind.Weather, WidgetSize.Small),
            HomeWidget("w-energy", WidgetKind.Energy, WidgetSize.Medium),
        ),
        widgetSuggestion = null,
        vehicles = listOf(
            Vehicle("khalid-landcruiser", "Toyota", "Land Cruiser", 2023, "Pearl white", cruiser, DemoDate.fixed(2027, 2, 18), "Dubai Insurance",
                DemoDate.fixed(2027, 2, 11), DemoDate.fixed(2026, 2, 16), "Tag ending 5140"),
        ),
        documents = listOf(
            DocumentItem("khalid-eid", "Emirates ID", "784-1985-•••••••-1", DemoDate.fixed(2030, 5, 12), symbol = "person.text.rectangle.fill", hue = Hue.Mint),
            DocumentItem("khalid-passport", "Passport", "United Arab Emirates · •••••3307", DemoDate.fixed(2029, 11, 4), symbol = "book.closed.fill", hue = Hue.Sand),
            DocumentItem("khalid-familybook", "Family book", "Khulasat Al Qaid · •••214", null, symbol = "person.2.fill", hue = Hue.Peach),
            DocumentItem("khalid-licence", "Driving licence", "Licence ending 2290", DemoDate.fixed(2034, 3, 9), symbol = "car.fill", hue = Hue.Sky),
            DocumentItem("khalid-mulkiya", "Vehicle registration", "Land Cruiser · L 7340", DemoDate.fixed(2027, 2, 18), symbol = "doc.text.fill", hue = Hue.Sky),
            DocumentItem("khalid-plot", "Title deed", "Plot in Al Khawaneej 2 · 2245-•••", null, symbol = "map.fill", hue = Hue.Sand),
        ),
        // UAE nationals: no visas, so nobody is sponsored and there's no Family visas status.
        family = listOf(
            FamilyMember("khalid-maryam", "Maryam Al Mansoori", "Wife", 38, null, false, "M", Hue.Lilac),
            FamilyMember("khalid-hamdan", "Hamdan Al Mansoori", "Son", 12, null, false, "H", Hue.Sky),
            FamilyMember("khalid-fatima", "Fatima Al Mansoori", "Daughter", 9, null, false, "F", Hue.Peach),
            FamilyMember("khalid-saif", "Saif Al Mansoori", "Son", 4, null, false, "S", Hue.Mint),
        ),
        familyVisas = null,
        home = HomeInfo("Villa 14, Street 23, Al Mizhar 1", "2003 5521", null, "29561 87430"),
        paymentMethods = listOf(PaymentMethod.GooglePay, PaymentMethod.Card("Mastercard", "7710")),
        requests = listOf(
            RequestItem(
                "khalid-letter", "get-housing-support-letter", "Get a letter about your housing support", "MBRHE", "MBRHE-HL-26-01942",
                RequestItem.Status.InReview(DemoDate.day(2)), null,
                listOf(
                    RequestStep("Ask for the letter", "${DemoDate.short(DemoDate.day(-1))} · for your consultant, Dar Sidra Engineering", RequestStep.State.Done),
                    RequestStep("MBRHE prepares your letter", "Answer by ${DemoDate.short(DemoDate.day(2))}", RequestStep.State.Current),
                    RequestStep("Download your letter", "We'll add it to your documents", RequestStep.State.Upcoming),
                ),
                DemoDate.day(-1), "house.fill", Hue.Sand,
            ),
            RequestItem(
                "khalid-loan", "apply-building-loan", "Apply for a loan to build a home", "MBRHE", "MBRHE-BL-26-00731",
                RequestItem.Status.Done, null,
                listOf(
                    RequestStep("Apply with your plot and family details", "${DemoDate.short(DemoDate.fixed(2026, 5, 11))} · title deed found automatically", RequestStep.State.Done),
                    RequestStep("MBRHE checks your application", "${DemoDate.short(DemoDate.fixed(2026, 6, 24))} · site visit in Al Khawaneej 2", RequestStep.State.Done),
                    RequestStep("Your loan is approved", "${DemoDate.short(DemoDate.fixed(2026, 7, 16))} · AED 800,000, interest-free", RequestStep.State.Done),
                    RequestStep("Sign the loan agreement", "${DemoDate.short(DemoDate.fixed(2026, 7, 23))} · paid in stages as you build", RequestStep.State.Done),
                ),
                DemoDate.fixed(2026, 5, 11), "house.fill", Hue.Sand,
            ),
        ),
        drafts = listOf(
            DraftItem("khalid-draft-zakat", "pay-zakat", "Work out and pay your zakat", 1, 3, DemoDate.day(-8), "heart.fill", Hue.Lilac),
        ),
        history = listOf(
            Receipt("khalid-r-donate", "Donated to Dar Al Ber Society", "Donated to Dar Al Ber Society", "Dar Al Ber Society", 500.aed,
                DemoDate.day(-12, 21, 18), PaymentMethod.GooglePay, "DAB-26-310557", listOf(ReceiptRow("For", "Family support")),
                "heart.fill", Hue.Lilac, "donate-to-charity"),
            Receipt("khalid-r-salik", "Topped up your Salik account", "Topped up Salik", "Salik", 100.aed, DemoDate.day(-22, 7, 36), PaymentMethod.GooglePay,
                "SLK-26-4402", listOf(ReceiptRow("New balance", "AED 146.00")), "road.lanes", Hue.Sand, "top-up-salik-account"),
            Receipt("khalid-r-dewa", "Paid your electricity and water bill", "Paid electricity and water", "DEWA", 452.80.aed, DemoDate.day(-26, 20, 12),
                PaymentMethod.GooglePay, "DEWA-2003-5521-0826", listOf(ReceiptRow("For", "August · Al Mizhar 1")), "bolt.fill", Hue.Sand,
                "pay-electricity-water-bill", rating = HappinessFace.Happy),
            Receipt("khalid-r-loan", "Applied for a loan to build a home", "Applied for a building loan", "MBRHE", null,
                DemoDate.fixed(2026, 5, 11, hour = 10), null, "MBRHE-BL-26-00731",
                listOf(ReceiptRow("Plot", "Al Khawaneej 2"), ReceiptRow("Outcome", "Approved 16 July 2026")),
                "house.fill", Hue.Sand, "apply-building-loan", rating = HappinessFace.Happy),
        ),
        // 2 unread → bell badge 2.
        notifications = listOf(
            NotificationItem("khalid-n-drawings", "Your villa drawings are ready to review",
                "Dar Sidra Engineering sent the drawings for your house in Al Khawaneej 2. Approve them to apply for your building permit.",
                DemoDate.day(0, 8, 30), "house.lodge.fill", Hue.Sand, false, Route.Journey("build-a-house")),
            NotificationItem("khalid-n-dewa", "Your September bill is ready", "Electricity and water: AED 386.20, due ${DemoDate.long(DemoDate.day(5))}.",
                DemoDate.day(-1, 10, 30), "bolt.fill", Hue.Sand, false, null),
            NotificationItem("khalid-n-letter", "MBRHE is preparing your letter", "Your housing support letter will be ready by ${DemoDate.long(DemoDate.day(2))}.",
                DemoDate.day(-1, 9, 4), "house.fill", Hue.Sand, true, Route.Request("khalid-letter")),
            NotificationItem("khalid-n-donate", "Thank you for your donation", "AED 500 to Dar Al Ber Society. Your receipt is in History.",
                DemoDate.day(-12, 21, 19), "heart.fill", Hue.Lilac, true, Route.Receipt("khalid-r-donate")),
        ),
        linkedAccounts = listOf(
            LinkedAccount("dewa", "DEWA", "Electricity and water · 2003 5521", "bolt.fill", Hue.Sand, true),
            LinkedAccount("rta", "RTA", "1 vehicle and your driving licence", "car.fill", Hue.Sky, true),
            LinkedAccount("police", "Dubai Police", "Traffic fines", "shield.fill", Hue.Rose, true),
            LinkedAccount("salik", "Salik", "1 tag", "road.lanes", Hue.Sand, true),
            LinkedAccount("mbrhe", "MBRHE", "Housing loan · approved July", "house.fill", Hue.Sand, true),
            LinkedAccount("dm", "Dubai Municipality", "Building drawings and permits", "hammer.fill", Hue.Sand, true),
            LinkedAccount("du", "du", "Mobile ending 1200", "phone.fill", Hue.Sand, true),
            LinkedAccount("eand", "e&", "Not linked", "wifi", Hue.Sand, false),
        ),
        salikBalance = 96.aed,
        nolBalance = null,
        parking = null,
        energy = EnergyReading("September", 2180, -16, "Lower than 57% of similar homes in Al Mizhar", listOf(1960, 2540, 2890, 3020, 2610, 2180)),
        metro = null,
        journeys = listOf(
            JourneyProgress("build-a-house", 3, "Review your villa drawings", "Sent by your consultant today",
                DemoDate.fixed(2026, 1, 25), "in Al Khawaneej 2", needsYou = true),
        ),
    )
}
