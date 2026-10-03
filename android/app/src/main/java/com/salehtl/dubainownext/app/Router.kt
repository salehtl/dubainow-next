package com.salehtl.dubainownext.app

import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.salehtl.dubainownext.data.Catalogue
import com.salehtl.dubainownext.data.CatalogueService
import com.salehtl.dubainownext.data.PaymentRequest
import com.salehtl.dubainownext.data.ServiceType
import com.salehtl.dubainownext.design.components.ComingSoon

/** The three tabs plus search (on Android, search opens a full-screen M3 search view). */
enum class AppTab { Home, Services, Activity, Search }

/** Every pushed screen. Each tab has its own back stack of these (see RouteDestinations.kt). */
sealed interface Route {
    // Services catalogue
    data class Topic(val id: String) : Route
    data class Subtopic(val topic: String, val sub: String) : Route
    data class Service(val id: String) : Route
    data class Bundle(val id: String) : Route
    data class Journey(val id: String) : Route
    data object AllServices : Route
    data object Entities : Route
    data class Entity(val name: String) : Route
    data object BundlesList : Route
    data object JourneysList : Route
    // Things
    data class Vehicle(val id: String) : Route
    data object Vehicles : Route
    data object Documents : Route
    data class Document(val id: String) : Route
    data object Family : Route
    // Activity
    data class Request(val id: String) : Route
    data class Receipt(val id: String) : Route
    data object Drafts : Route
    data object History : Route
    data object Notifications : Route

    companion object {
        /** "topic:driving-and-transport", "service:pay-traffic-fine", "allServices", … (launch extras) */
        fun parse(s: String): Route? {
            val key = s.substringBefore(":"); val v = s.substringAfter(":", "")
            return when (key) {
                "topic" -> Topic(v)
                "subtopic" -> v.split("/").takeIf { it.size == 2 }?.let { Subtopic(it[0], it[1]) }
                "service" -> Service(v); "bundle" -> Bundle(v); "journey" -> Journey(v)
                "allServices" -> AllServices; "entities" -> Entities; "entity" -> Entity(v)
                "bundlesList" -> BundlesList; "journeysList" -> JourneysList
                "vehicle" -> Vehicle(v); "vehicles" -> Vehicles; "documents" -> Documents; "document" -> Document(v)
                "family" -> Family; "request" -> Request(v); "receipt" -> Receipt(v)
                "drafts" -> Drafts; "history" -> History; "notifications" -> Notifications
                else -> null
            }
        }
    }
}

/** Bottom sheets presented over everything (M3 modal bottom sheets). */
sealed interface SheetRoute {
    data object Profile : SheetRoute
    data class HappinessMeter(val context: String? = null) : SheetRoute
    data object Notifications : SheetRoute
    data class Payment(val request: PaymentRequest) : SheetRoute
    data class Receipt(val id: String) : SheetRoute
    data object WidgetGallery : SheetRoute
    data object EditFavorites : SheetRoute
    data class ComingSoonSheet(val item: ComingSoon) : SheetRoute
    data object ExtendParking : SheetRoute
    data object TopUpSalik : SheetRoute
    data object DemoMenu : SheetRoute

    companion object {
        fun parse(s: String): SheetRoute? = when (s.substringBefore(":")) {
            "profile" -> Profile; "happiness" -> HappinessMeter(); "notifications" -> Notifications
            "receipt" -> Receipt(s.substringAfter(":", "")); "widgetGallery" -> WidgetGallery
            "editFavorites" -> EditFavorites; "extendParking" -> ExtendParking; "topUpSalik" -> TopUpSalik
            "demoMenu" -> DemoMenu
            "comingSoon" -> ComingSoonSheet(ComingSoon(title = "Renew your vehicle registration", owner = "RTA", symbol = "car.fill"))
            else -> null
        }
    }
}

/**
 * Navigation state for the whole app. Features call [push], [present], [select] and
 * [openAssistant] instead of owning navigation state for cross-feature moves.
 */
class Router {
    var tab by mutableStateOf(AppTab.Home)
    private val stacks: Map<AppTab, MutableList<Route>> = AppTab.entries.associateWith { mutableStateListOf() }

    var sheet by mutableStateOf<SheetRoute?>(null)
    /** The search query (the search view's text field). */
    var searchQuery by mutableStateOf("")
    /** The assistant overlay (covers the UI; the screen underneath stays live). */
    var isAssistantPresented by mutableStateOf(false)
    /** Optional text to start the assistant with ("__listen__" = opened by touch and hold). */
    var assistantSeed by mutableStateOf<String?>(null)
    /** Set by a widget's "Edit Home" menu item; Home enters edit mode and clears it. */
    var homeEditRequested by mutableStateOf(false)
    /** Set by Home while editing: the tab bar hides, as on iOS. */
    var hidesTabBar by mutableStateOf(false)
    /** The tab search was opened from; back from search returns there (M3 search view behaviour). */
    var searchOrigin by mutableStateOf(AppTab.Home)
        private set

    fun stack(tab: AppTab): List<Route> = stacks.getValue(tab)
    val current: Route? get() = stacks.getValue(tab).lastOrNull()

    /** Push onto the current tab's stack. */
    fun push(route: Route) { stacks.getValue(tab).add(route) }

    /** Back one screen on the current tab. Returns false at the root. */
    fun pop(): Boolean {
        val s = stacks.getValue(tab)
        if (s.isEmpty()) return false
        s.removeAt(s.lastIndex); return true
    }

    fun popToRoot() { stacks.getValue(tab).clear() }

    private val reselects: Map<AppTab, MutableIntState> = AppTab.entries.associateWith { mutableIntStateOf(0) }

    /** How many times [tab] was tapped again while at its root (its root scrolls to the top; see [ScrollToTopOnReselect]). */
    fun reselectCount(tab: AppTab): Int = reselects.getValue(tab).intValue

    /** The selected tab, tapped again at its root. */
    fun reselect(tab: AppTab) { reselects.getValue(tab).intValue++ }

    /** Switch tab and optionally push a route there. */
    fun select(tab: AppTab, push: Route? = null, resetPath: Boolean = false) {
        if (tab == AppTab.Search && this.tab != AppTab.Search) searchOrigin = this.tab
        this.tab = tab
        if (resetPath) stacks.getValue(tab).clear()
        push?.let { push(it) }
    }

    fun openSearch() = select(AppTab.Search)
    fun closeSearch() { tab = searchOrigin }

    fun present(sheet: SheetRoute) { this.sheet = sheet }
    fun dismissSheet() { sheet = null }

    fun openAssistant(seed: String? = null) { assistantSeed = seed; isAssistantPresented = true }
    fun closeAssistant() { isAssistantPresented = false; assistantSeed = null }

    /** Open whatever a catalogue record is: start page, bundle page or journey page. */
    fun open(service: CatalogueService) {
        when (service.type) {
            ServiceType.Standalone -> push(if (service.kind == "thing") thingRoute(service.id) else Route.Service(service.id))
            ServiceType.Bundle -> push(Route.Bundle(service.id))
            ServiceType.Journey -> push(Route.Journey(service.id))
        }
    }

    fun open(serviceID: String) { Catalogue.service(serviceID)?.let { open(it) } }

    private fun thingRoute(id: String): Route = when (id) {
        "your-vehicles" -> Route.Vehicles
        "your-documents" -> Route.Documents
        "your-family-residency" -> Route.Family
        else -> Route.Service(id)
    }

    fun reset() {
        tab = AppTab.Home
        stacks.values.forEach { it.clear() }
        sheet = null; isAssistantPresented = false; assistantSeed = null; searchQuery = ""
        hidesTabBar = false; searchOrigin = AppTab.Home
    }
}
