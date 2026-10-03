package com.salehtl.dubainownext.data

import androidx.compose.ui.graphics.vector.ImageVector
import com.salehtl.dubainownext.design.DNIcons
import com.salehtl.dubainownext.design.Hue

/**
 * How a catalogue record looks: its hue and icon.
 * Rule (C1 · Decisions): a service's hue belongs to its record and defaults to its topic.
 * The catalogue has no icon field, so each service uses its sub-topic's icon unless overridden.
 * Symbol names are the iOS names (shared with the iOS app); render with [DNIcons.forSymbol].
 */
object ServiceVisuals {
    val topicHue = mapOf(
        "driving-and-transport" to Hue.Sky, "visas-and-residency" to Hue.Lilac, "housing-and-utilities" to Hue.Sand,
        "health" to Hue.Mint, "family-and-social-support" to Hue.Peach, "births-marriages-and-deaths" to Hue.Rose,
        "work-and-business" to Hue.Sand, "education-and-learning" to Hue.Sky, "faith-and-giving" to Hue.Lilac,
        "city-and-environment" to Hue.Mint, "police-justice-and-the-law" to Hue.Rose, "leisure-culture-and-events" to Hue.Peach,
    )

    val topicSymbol = mapOf(
        "driving-and-transport" to "car.fill", "visas-and-residency" to "person.text.rectangle.fill",
        "housing-and-utilities" to "house.fill", "health" to "heart.fill", "family-and-social-support" to "person.2.fill",
        "births-marriages-and-deaths" to "figure.2.and.child.holdinghands", "work-and-business" to "briefcase.fill",
        "education-and-learning" to "graduationcap.fill", "faith-and-giving" to "moon.stars.fill",
        "city-and-environment" to "leaf.fill", "police-justice-and-the-law" to "building.columns.fill",
        "leisure-culture-and-events" to "ticket.fill",
    )

    val subtopicSymbol = mapOf(
        "driving-and-transport/vehicles" to "car.fill", "driving-and-transport/licences" to "person.text.rectangle.fill",
        "driving-and-transport/fines" to "exclamationmark.triangle.fill", "driving-and-transport/parking" to "p.square.fill",
        "driving-and-transport/plates" to "number.square.fill", "driving-and-transport/roads" to "road.lanes",
        "driving-and-transport/public-transport" to "tram.fill", "driving-and-transport/air" to "airplane",
        "visas-and-residency/your-visa" to "person.text.rectangle.fill", "visas-and-residency/family" to "person.2.fill",
        "visas-and-residency/ids" to "person.crop.rectangle",
        "housing-and-utilities/renting" to "key.fill", "housing-and-utilities/utilities" to "bolt.fill",
        "housing-and-utilities/phone-internet" to "wifi", "housing-and-utilities/owning" to "house.fill",
        "housing-and-utilities/building" to "hammer.fill", "housing-and-utilities/support" to "building.2.fill",
        "health/care" to "stethoscope", "health/records" to "doc.text.fill", "health/insurance" to "cross.case.fill",
        "health/emergencies" to "staroflife.fill",
        "family-and-social-support/financial" to "banknote.fill", "family-and-social-support/pod" to "figure.roll",
        "family-and-social-support/older" to "figure.walk", "family-and-social-support/family-life" to "figure.2.and.child.holdinghands",
        "births-marriages-and-deaths/births" to "stroller.fill", "births-marriages-and-deaths/marriage" to "heart.circle.fill",
        "births-marriages-and-deaths/death" to "leaf.fill",
        "work-and-business/your-job" to "briefcase.fill", "work-and-business/business" to "storefront.fill",
        "work-and-business/health-pros" to "stethoscope", "work-and-business/money" to "banknote.fill",
        "education-and-learning/schools" to "backpack.fill", "education-and-learning/higher" to "graduationcap.fill",
        "faith-and-giving/prayer" to "moon.stars.fill", "faith-and-giving/giving" to "heart.fill",
        "faith-and-giving/endowments" to "building.columns.fill", "faith-and-giving/guidance" to "book.fill",
        "city-and-environment/report" to "exclamationmark.bubble.fill", "city-and-environment/waste" to "trash.fill",
        "city-and-environment/parks" to "tree.fill", "city-and-environment/animals" to "pawprint.fill",
        "city-and-environment/streets" to "map.fill", "city-and-environment/payments" to "creditcard.fill",
        "city-and-environment/post" to "envelope.fill",
        "police-justice-and-the-law/emergencies" to "phone.fill", "police-justice-and-the-law/report" to "exclamationmark.bubble.fill",
        "police-justice-and-the-law/police" to "shield.fill", "police-justice-and-the-law/courts" to "building.columns.fill",
        "leisure-culture-and-events/events" to "ticket.fill", "leisure-culture-and-events/culture" to "theatermasks.fill",
        "leisure-culture-and-events/sport" to "sportscourt.fill", "leisure-culture-and-events/community" to "person.3.fill",
        "leisure-culture-and-events/offers" to "tag.fill",
    )

    /** Record-level overrides (drawn on the Paper screens). */
    val serviceOverrides: Map<String, Pair<String, Hue?>> = mapOf(
        "pay-for-parking" to ("p.square.fill" to Hue.Sky),
        "top-up-salik-account" to ("road.lanes" to Hue.Sand),
        "activate-salik-tag" to ("road.lanes" to Hue.Sand),
        "donate-to-charity" to ("heart.fill" to Hue.Lilac),
        "your-vehicles" to ("car.fill" to Hue.Rose),
        "renew-vehicle-registration" to ("car.fill" to Hue.Sky),
        "pay-traffic-fine" to ("exclamationmark.triangle.fill" to Hue.Sky),
        "pay-electricity-water-bill" to ("bolt.fill" to Hue.Sand),
        "renew-residence-visa" to ("person.text.rectangle.fill" to Hue.Lilac),
        "check-visa-status" to ("person.text.rectangle.fill" to Hue.Lilac),
        "top-up-nol-card" to ("tram.fill" to Hue.Sky),
        "pay-du-bill" to ("phone.fill" to Hue.Sand),
        "pay-etisalat-bill" to ("phone.fill" to Hue.Sand),
        "check-prayer-times" to ("moon.stars.fill" to Hue.Lilac),
        "report-employer-problem" to ("exclamationmark.bubble.fill" to Hue.Sand),
        "get-good-conduct-certificate" to ("checkmark.shield.fill" to Hue.Rose),
        "register-tenancy-contract" to ("key.fill" to Hue.Sand),
        "pay-all" to ("creditcard.fill" to Hue.Sky),
        "get-certificates" to ("doc.on.doc.fill" to Hue.Lilac),
        "renew-family-visas" to ("person.3.fill" to Hue.Lilac),
        "apply-all-pod-benefits" to ("figure.roll" to Hue.Peach),
        "build-a-house" to ("house.lodge.fill" to Hue.Sand),
        "move-home" to ("shippingbox.fill" to Hue.Sand),
        "get-married" to ("heart.circle.fill" to Hue.Rose),
        "after-a-death" to ("leaf.fill" to Hue.Rose),
        "learn-to-drive" to ("steeringwheel" to Hue.Sky),
    )

    fun hue(forTopic: String?): Hue = topicHue[forTopic] ?: Hue.Sky
    fun hue(s: CatalogueService): Hue = serviceOverrides[s.id]?.second ?: hue(s.topic)

    fun symbol(s: CatalogueService): String =
        serviceOverrides[s.id]?.first
            ?: s.topic?.let { t -> s.subtopic?.let { subtopicSymbol["$t/$it"] } }
            ?: s.topic?.let { topicSymbol[it] }
            ?: "square.grid.2x2.fill"

    fun symbol(forTopic: String): String = topicSymbol[forTopic] ?: "square.grid.2x2.fill"
    fun symbol(topic: String, subtopic: String): String = subtopicSymbol["$topic/$subtopic"] ?: symbol(topic)
}

val CatalogueService.hue: Hue get() = ServiceVisuals.hue(this)
val CatalogueService.symbol: String get() = ServiceVisuals.symbol(this)
val CatalogueService.icon: ImageVector get() = DNIcons.forSymbol(symbol)
val CatalogueTopic.hue: Hue get() = ServiceVisuals.hue(id)
val CatalogueTopic.symbol: String get() = ServiceVisuals.symbol(id)
val CatalogueTopic.icon: ImageVector get() = DNIcons.forSymbol(symbol)
