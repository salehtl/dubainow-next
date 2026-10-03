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
import com.salehtl.dubainownext.data.JourneyProgress
import com.salehtl.dubainownext.data.LinkedAccount
import com.salehtl.dubainownext.data.MetroDeparture
import com.salehtl.dubainownext.data.MetroStation
import com.salehtl.dubainownext.data.NeedsYouItem
import com.salehtl.dubainownext.data.NotificationItem
import com.salehtl.dubainownext.data.ParkingSession
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
import java.math.BigDecimal

// Sara Haddad — the main demo persona (Paper C1 scenario). Ported from iOS Sara.swift; same numbers:
//   Needs you = 4 (2 due this week): DEWA AED 412.60 due tomorrow · passport upload for the visa
//   (step 3 of 5, due in 2 days) · speeding fine AED 600 (blocks the car renewal) · Patrol registration due in 12 days.
//   Pay all = DEWA + fine = AED 1,012.60. Renewal through the assistant = AED 420 + 600 = AED 1,020.
//   Salik: topped up AED 100 thirteen days ago (balance then AED 118), AED 18 left now.

val sara: Persona
    get() = Persona(
        id = "sara", firstName = "Sara", lastName = "Haddad", initials = "S", avatarHue = Hue.Peach,
        tagline = "Mum of two in Al Barsha. Car registration due, visa renewal in progress, moving home.",
        area = "Al Barsha 1", residency = "Residence visa", emiratesID = "784-1990-•••••••-3", phone = "+971 50 ••• 4821",
        assistantPrompts = listOf("Renew my car registration", "How much do I owe?", "When will my visa renewal be done?", "Top up Salik with AED 100"),
        makeState = { saraState() },
    )

fun saraState(): PersonaState {
    val dewa = Payable(
        id = "sara-dewa-sep", kind = Payable.Kind.Bill, title = "Electricity & water bill", entity = "DEWA",
        detail = "Al Barsha 1 · Account 2004 1187", amount = 412.60.aed, due = DemoDate.day(1),
        reference = "DEWA-2004-1187-0926", symbol = "bolt.fill", hue = Hue.Sand, serviceID = "pay-electricity-water-bill",
    )
    val fine = Payable(
        id = "sara-fine-speeding", kind = Payable.Kind.Fine, title = "Speeding fine", entity = "Dubai Police",
        detail = "Sheikh Zayed Road · ${DemoDate.short(DemoDate.day(-18))}", amount = 600.aed, due = null,
        reference = "DXB-TF-26-3318042", symbol = "gauge.with.dots.needle.67percent", hue = Hue.Sky,
        serviceID = "pay-traffic-fine", note = "Blocks your car renewal",
    )
    val patrol = Plate(code = "M", number = "48213")
    return PersonaState(
        needsYou = listOf(
            NeedsYouItem("ny-dewa", NeedsYouItem.Kind.Payment(dewa), "DEWA", "bolt.fill", Hue.Sand,
                "Electricity & water bill", "Al Barsha 1 · Account 2004 1187", DemoDate.day(1), "Pay now", "Later"),
            NeedsYouItem("ny-visa", NeedsYouItem.Kind.RequestStep("sara-visa"), "GDRFA", "person.text.rectangle.fill", Hue.Lilac,
                "Upload a clearer passport copy", "Renew your residence visa", DemoDate.day(2), "Upload now", "Details",
                progress = StepProgress(3, 5)),
            NeedsYouItem("ny-fine", NeedsYouItem.Kind.Payment(fine), "Dubai Police", "gauge.with.dots.needle.67percent", Hue.Sky,
                "Speeding fine", "Sheikh Zayed Road · blocks your car renewal", null, "Pay now", "Details"),
            NeedsYouItem("ny-patrol", NeedsYouItem.Kind.Renewal("renew-vehicle-registration", "sara-patrol"), "RTA", "car.fill", Hue.Sky,
                "Renew your Patrol's registration", "M 48213 · expires ${DemoDate.short(DemoDate.day(12))}", DemoDate.day(12), "Renew", "Later"),
        ),
        favorites = listOf(
            FavoriteItem("fav-parking", "Pay parking", "p.square.fill", Hue.Sky, FavoriteItem.Target.Service("pay-for-parking")),
            FavoriteItem("fav-salik", "Salik", "road.lanes", Hue.Sand, FavoriteItem.Target.Service("top-up-salik-account")),
            FavoriteItem("fav-vehicles", "Vehicles", "car.fill", Hue.Rose, FavoriteItem.Target.Thing("your-vehicles")),
            FavoriteItem("fav-donate", "Donate", "heart.fill", Hue.Lilac, FavoriteItem.Target.Service("donate-to-charity")),
        ),
        widgets = listOf(
            HomeWidget("w-parking", WidgetKind.Parking, WidgetSize.Small),
            HomeWidget("w-salik", WidgetKind.Salik, WidgetSize.Small),
            HomeWidget("w-prayer", WidgetKind.Prayer, WidgetSize.Small),
            HomeWidget("w-weather", WidgetKind.Weather, WidgetSize.Small),
            HomeWidget("w-metro", WidgetKind.Metro, WidgetSize.Medium),
            HomeWidget("w-energy", WidgetKind.Energy, WidgetSize.Medium),
        ),
        widgetSuggestion = WidgetKind.FamilyVisas,
        vehicles = listOf(
            Vehicle("sara-patrol", "Nissan", "Patrol", 2022, "White", patrol, DemoDate.day(12), "Sukoon Insurance",
                DemoDate.fixed(2027, 3, 14), DemoDate.day(-12), "Tag ending 3391", listOf("sara-fine-speeding")),
            Vehicle("sara-corolla", "Toyota", "Corolla", 2019, "Silver", Plate(code = "K", number = "30577"), DemoDate.fixed(2027, 5, 9),
                "Sukoon Insurance", DemoDate.fixed(2027, 5, 2), DemoDate.fixed(2026, 5, 6), "Tag ending 8820"),
        ),
        documents = listOf(
            DocumentItem("sara-eid", "Emirates ID", "784-1990-•••••••-3", DemoDate.fixed(2028, 3, 21), symbol = "person.text.rectangle.fill", hue = Hue.Mint),
            DocumentItem("sara-visa-doc", "Residence visa", "201/2023/•••••42", DemoDate.day(47), DocumentItem.Status.Renewing, "Renewing · your turn", "person.text.rectangle", Hue.Lilac),
            DocumentItem("sara-passport", "Passport", "Lebanon · RL••••821", DemoDate.fixed(2031, 6, 30), symbol = "book.closed.fill", hue = Hue.Sand),
            DocumentItem("sara-licence", "Driving licence", "Licence ending 6630", DemoDate.fixed(2036, 9, 22), DocumentItem.Status.Renewing,
                "New card arrives ${DemoDate.weekday(DemoDate.day(2))}", "car.fill", Hue.Sky),
            DocumentItem("sara-mulkiya", "Vehicle registration", "Patrol · M 48213", DemoDate.day(12), DocumentItem.Status.ExpiringSoon, "Expires in 12 days", "doc.text.fill", Hue.Sky),
            DocumentItem("sara-ejari", "Tenancy contract", "Ejari · Al Barsha 1", DemoDate.fixed(2027, 1, 14), symbol = "key.fill", hue = Hue.Sand),
        ),
        family = listOf(
            FamilyMember("sara-ahmed", "Ahmed Haddad", "Husband", 41, DemoDate.fixed(2027, 8, 3), true, "A", Hue.Sky),
            FamilyMember("sara-layla", "Layla Haddad", "Daughter", 9, DemoDate.day(73), true, "L", Hue.Peach),
            FamilyMember("sara-omar", "Omar Haddad", "Son", 6, DemoDate.day(73), true, "O", Hue.Mint),
        ),
        familyVisas = FamilyVisasStatus(listOf("sara-ahmed", "sara-layla", "sara-omar"), listOf("sara-layla", "sara-omar"), DemoDate.day(73)),
        home = HomeInfo("Villa 23, Street 15, Al Barsha 1", "2004 1187", DemoDate.fixed(2027, 1, 14), "17845 41205"),
        paymentMethods = listOf(PaymentMethod.GooglePay, PaymentMethod.Card("Visa", "4021")),
        requests = listOf(
            RequestItem(
                "sara-visa", "renew-residence-visa", "Renew your residence visa", "GDRFA", "GDRFA-R-26-0042817",
                RequestItem.Status.YourTurn(DemoDate.day(2)),
                "We need a clearer passport copy" to "The last photo was blurry around the passport number. Upload a new one by ${DemoDate.long(DemoDate.day(2))} and GDRFA will review your application within 2 working days.",
                listOf(
                    RequestStep("Pass your medical fitness test", "${DemoDate.short(DemoDate.day(-8))} · DHA, Al Barsha centre", RequestStep.State.Done),
                    RequestStep("Confirm your health insurance", "${DemoDate.short(DemoDate.day(-8))} · found automatically, valid to 2027", RequestStep.State.Done),
                    RequestStep("Upload your passport copy", "Your turn · by ${DemoDate.long(DemoDate.day(2))}", RequestStep.State.Current),
                    RequestStep("Pay the fees", "About AED 1,150 · we'll show the exact amount", RequestStep.State.Upcoming),
                    RequestStep("GDRFA reviews your application", "Usually 2 working days · we'll notify you", RequestStep.State.Upcoming),
                ),
                DemoDate.day(-9), "person.text.rectangle.fill", Hue.Lilac, action = "Upload now",
            ),
            RequestItem(
                "sara-licence", "renew-driving-licence", "Renew your driving licence", "RTA", "RTA-DL-26-118204",
                RequestItem.Status.Approved("card arrives ${DemoDate.weekday(DemoDate.day(2))}"), null,
                listOf(
                    RequestStep("Pass an eye test", "${DemoDate.short(DemoDate.day(-11))} · Al Barsha optician", RequestStep.State.Done),
                    RequestStep("Pay the fee", "${DemoDate.short(DemoDate.day(-10))} · AED 300", RequestStep.State.Done),
                    RequestStep("RTA approves your renewal", DemoDate.short(DemoDate.day(-1)), RequestStep.State.Done),
                    RequestStep("Your new card is delivered", "Arrives ${DemoDate.weekday(DemoDate.day(2))} · Emirates Post", RequestStep.State.Current),
                ),
                DemoDate.day(-11), "person.text.rectangle.fill", Hue.Sky,
            ),
            RequestItem(
                "sara-salik-dispute", "top-up-salik-account", "Dispute a Salik charge", "Salik", "SLK-DSP-26-5512",
                RequestItem.Status.InReview(DemoDate.day(7)), null,
                listOf(
                    RequestStep("Tell Salik what happened", "${DemoDate.short(DemoDate.day(-3))} · charged twice at Al Safa", RequestStep.State.Done),
                    RequestStep("Salik reviews the trips", "Answer by ${DemoDate.short(DemoDate.day(7))}", RequestStep.State.Current),
                    RequestStep("Refund to your Salik balance", "AED 4 if approved", RequestStep.State.Upcoming),
                ),
                DemoDate.day(-3), "road.lanes", Hue.Sand,
            ),
        ),
        drafts = listOf(
            DraftItem("sara-draft-ejari", "register-tenancy-contract", "Register your tenancy contract (Ejari)", 2, 4, DemoDate.day(-4), "key.fill", Hue.Sand),
            DraftItem("sara-draft-conduct", "get-good-conduct-certificate", "Get a certificate of good conduct", 1, 3, DemoDate.day(-6), "checkmark.shield.fill", Hue.Rose),
        ),
        history = listOf(
            Receipt("sara-r-parking", "Paid for parking", "Paid for parking", "Parkin", 34.aed, DemoDate.day(-4, 18, 2), PaymentMethod.GooglePay,
                "PKN-26-77120", listOf(ReceiptRow("Sessions", "6 in Al Barsha and DIFC")), "p.square.fill", Hue.Sky, "pay-for-parking", count = 6),
            Receipt("sara-r-licence", "Applied to renew your driving licence", "Applied to renew driving licence", "RTA", 300.aed, DemoDate.day(-10, 20, 41),
                PaymentMethod.Card("Visa", "4021"), "RTA-DL-26-118204", emptyList(), "person.text.rectangle.fill", Hue.Sky, "renew-driving-licence"),
            Receipt("sara-r-salik", "Topped up your Salik account", "Topped up Salik", "Salik", 100.aed, DemoDate.day(-13, 8, 14), PaymentMethod.GooglePay,
                "SLK-26-7731", listOf(ReceiptRow("New balance", "AED 118.00")), "road.lanes", Hue.Sand, "top-up-salik-account"),
            Receipt("sara-r-dewa", "Paid your electricity and water bill", "Paid electricity and water", "DEWA", 398.20.aed, DemoDate.day(-28, 21, 5),
                PaymentMethod.GooglePay, "DEWA-2004-1187-0826", listOf(ReceiptRow("For", "August · Al Barsha 1")), "bolt.fill", Hue.Sand,
                "pay-electricity-water-bill", rating = HappinessFace.Happy),
        ),
        notifications = listOf(
            NotificationItem("sara-n-visa", "Upload a clearer passport copy", "GDRFA needs a new photo of your passport page by ${DemoDate.long(DemoDate.day(2))}.",
                DemoDate.day(0, 8, 2), "person.text.rectangle.fill", Hue.Lilac, false, Route.Request("sara-visa")),
            NotificationItem("sara-n-dewa", "Your September bill is ready", "Electricity and water: AED 412.60, due tomorrow.",
                DemoDate.day(-3, 10, 30), "bolt.fill", Hue.Sand, false, null),
            NotificationItem("sara-n-licence", "Driving licence renewal approved", "Your new card arrives on ${DemoDate.weekday(DemoDate.day(2))}.",
                DemoDate.day(-1, 16, 12), "checkmark.seal.fill", Hue.Mint, true, Route.Request("sara-licence")),
            NotificationItem("sara-n-salik", "Your Salik balance is low", "AED 18 left, about 4 trips.",
                DemoDate.day(-1, 7, 40), "road.lanes", Hue.Sand, true, null),
        ),
        linkedAccounts = listOf(
            LinkedAccount("dewa", "DEWA", "Electricity and water · 2004 1187", "bolt.fill", Hue.Sand, true),
            LinkedAccount("rta", "RTA", "2 vehicles and your driving licence", "car.fill", Hue.Sky, true),
            LinkedAccount("police", "Dubai Police", "Traffic fines", "shield.fill", Hue.Rose, true),
            LinkedAccount("salik", "Salik", "2 tags", "road.lanes", Hue.Sand, true),
            LinkedAccount("parkin", "Parkin", "Parking for 2 vehicles", "p.square.fill", Hue.Sky, true),
            LinkedAccount("du", "du", "Mobile ending 4821", "phone.fill", Hue.Sand, true),
            LinkedAccount("eand", "e&", "Not linked", "wifi", Hue.Sand, false),
        ),
        salikBalance = 18.aed,
        nolBalance = 46.50.aed,
        parking = ParkingSession("365C", "Al Barsha", patrol, DemoDate.minutes(-78), DemoDate.minutes(42), BigDecimal(4)),
        energy = EnergyReading("September", 1284, -12, "Lower than 68% of similar homes in Al Barsha", listOf(1610, 1702, 1745, 1690, 1459, 1284)),
        metro = MetroStation("Business Bay", "Red Line", listOf(MetroDeparture("Centrepoint", 3, 9), MetroDeparture("Expo 2020", 6, 14))),
        journeys = listOf(
            JourneyProgress("move-home", 1, "Sign the tenancy", "Waiting for your landlord to sign · sent yesterday", DemoDate.day(-4), "to Jumeirah Village Circle"),
        ),
    )
}
