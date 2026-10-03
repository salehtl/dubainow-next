import SwiftUI

/// The three tabs plus the search tab (iOS 26 search role: a separate glass circle).
enum AppTab: String, Hashable, CaseIterable {
    case home, services, activity, search
}

/// Every pushed screen in the app. Each tab has its own NavigationStack path of these.
/// Destinations are mapped in `RouteDestinations.swift` to the feature views.
enum Route: Hashable {
    // Services catalogue
    case topic(String)                         // topic id
    case subtopic(topic: String, sub: String)
    case service(String)                       // service start page (standalone)
    case bundle(String)                        // bundle page ("All at once")
    case journey(String)                       // journey page ("Start to finish")
    case allServices                           // A to Z
    case entities                              // By government entity
    case entity(String)                        // one owner's services
    case bundlesList                           // all bundles
    case journeysList                          // all journeys

    // Things
    case vehicle(String)                       // vehicle id
    case vehicles                              // "Your vehicles"
    case documents
    case document(String)
    case family

    // Activity
    case request(String)                       // request tracker
    case receipt(String)                       // receipt (pushed version)
    case drafts
    case history

    // Notifications (pushed when opened from a notification list inside a sheet)
    case notifications
}

/// Sheets presented over everything.
enum SheetRoute: Identifiable, Hashable {
    case profile
    case happinessMeter(context: String?)      // survey from the header smiley (context = a service)
    case notifications
    case payment(PaymentRequest)               // fake payment confirmation → receipt
    case receipt(String)                       // receipt by id
    case widgetGallery
    case editFavorites
    case comingSoon(ComingSoon)
    case extendParking
    case topUpSalik
    case demoMenu                              // switch persona, reset, time of day

    var id: String {
        switch self {
        case .profile: "profile"
        case .happinessMeter(let c): "happiness-\(c ?? "")"
        case .notifications: "notifications"
        case .payment(let p): "payment-\(p.id)"
        case .receipt(let id): "receipt-\(id)"
        case .widgetGallery: "widgetGallery"
        case .editFavorites: "editFavorites"
        case .comingSoon(let c): "comingSoon-\(c.id)"
        case .extendParking: "extendParking"
        case .topUpSalik: "topUpSalik"
        case .demoMenu: "demoMenu"
        }
    }
}

/// Navigation state for the whole app. Features call `push`, `present`, `select`
/// and `openAssistant` instead of owning navigation state for cross-feature moves.
@Observable
final class Router {
    var tab: AppTab = .home
    var homePath: [Route] = []
    var servicesPath: [Route] = []
    var activityPath: [Route] = []
    var searchPath: [Route] = []

    var sheet: SheetRoute?
    /// The search tab's query (bound to the tab view's native search field).
    var searchQuery = ""
    /// The assistant overlay (covers the UI; the screen underneath stays live).
    var isAssistantPresented = false
    /// Optional text to start the assistant with (e.g. a suggestion tapped elsewhere).
    var assistantSeed: String?

    /// Push onto the current tab's stack.
    func push(_ route: Route) {
        switch tab {
        case .home: homePath.append(route)
        case .services: servicesPath.append(route)
        case .activity: activityPath.append(route)
        case .search: searchPath.append(route)
        }
    }

    /// Switch tab and optionally push a route there.
    func select(_ tab: AppTab, push route: Route? = nil, resetPath: Bool = false) {
        self.tab = tab
        if resetPath { setPath([], for: tab) }
        if let route { push(route) }
    }

    func popToRoot() { setPath([], for: tab) }

    func present(_ sheet: SheetRoute) {
        if self.sheet != nil {
            // Replace an open sheet after it dismisses.
            self.sheet = nil
            Task { @MainActor in
                try? await Task.sleep(for: .milliseconds(450))
                self.sheet = sheet
            }
        } else {
            self.sheet = sheet
        }
    }

    func dismissSheet() { sheet = nil }

    func openAssistant(seed: String? = nil) {
        assistantSeed = seed
        withAnimation(DNMotion.resolve(DNMotion.smooth)) { isAssistantPresented = true }
    }

    func closeAssistant() {
        withAnimation(DNMotion.resolve(DNMotion.smooth)) { isAssistantPresented = false }
        assistantSeed = nil
    }

    /// Open whatever a catalogue record is: start page, bundle page or journey page.
    func open(service: CatalogueService) {
        switch service.type {
        case .standalone: push(service.kind == "thing" ? thingRoute(service.id) : .service(service.id))
        case .bundle: push(.bundle(service.id))
        case .journey: push(.journey(service.id))
        }
    }

    func open(serviceID: String) {
        guard let s = Catalogue.shared.service(serviceID) else { return }
        open(service: s)
    }

    private func thingRoute(_ id: String) -> Route {
        switch id {
        case "your-vehicles": .vehicles
        case "your-documents": .documents
        case "your-family-residency": .family
        default: .service(id)
        }
    }

    func reset() {
        tab = .home
        homePath = []; servicesPath = []; activityPath = []; searchPath = []
        sheet = nil
        isAssistantPresented = false
        assistantSeed = nil
    }

    private func setPath(_ path: [Route], for tab: AppTab) {
        switch tab {
        case .home: homePath = path
        case .services: servicesPath = path
        case .activity: activityPath = path
        case .search: searchPath = path
        }
    }
}
