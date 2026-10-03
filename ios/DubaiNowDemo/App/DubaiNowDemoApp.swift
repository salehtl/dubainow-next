import SwiftUI

@main
struct DubaiNowDemoApp: App {
    @State private var store = DemoStore()
    @State private var router = Router()
    @State private var toasts = ToastCenter()

    init() {
        let options = LaunchOptions.current
        let store = DemoStore()
        let router = Router()
        options.apply(store: store, router: router)
        _store = State(initialValue: store)
        _router = State(initialValue: router)
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .environment(store)
                .environment(router)
                .environment(toasts)
                .tint(DNColor.actionAccent)
        }
    }
}

/// Launch arguments for demos and screenshots, e.g.
///   -persona sara -tab services -route topic:driving-and-transport -sheet profile -assistant -hour 9
///   -welcome (start at the Welcome stories)
struct LaunchOptions {
    var persona: String?
    var tab: AppTab?
    var routes: [Route] = []
    var sheet: SheetRoute?
    var assistant = false
    var hour: Int?
    var welcome = false

    static var current: LaunchOptions {
        let defaults = UserDefaults.standard
        var o = LaunchOptions()
        o.persona = defaults.string(forKey: "persona")
        o.tab = defaults.string(forKey: "tab").flatMap(AppTab.init(rawValue:))
        if let r = defaults.string(forKey: "route") {
            o.routes = r.split(separator: ",").compactMap { Route(launchString: String($0)) }
        }
        o.sheet = defaults.string(forKey: "sheet").flatMap(SheetRoute.init(launchString:))
        o.assistant = ProcessInfo.processInfo.arguments.contains("-assistant")
        o.welcome = ProcessInfo.processInfo.arguments.contains("-welcome")
        if defaults.object(forKey: "hour") != nil { o.hour = defaults.integer(forKey: "hour") }
        return o
    }

    func apply(store: DemoStore, router: Router) {
        store.hourOverride = hour
        if !welcome, let id = persona {
            if id == "guest" { store.continueAsGuest() } else if let p = Personas.persona(id: id) { store.signIn(p) }
        }
        if let tab { router.tab = tab }
        for r in routes { router.push(r) }
        if case .payment(let p) = sheet, p.id == "launch-pay-all" {
            router.sheet = .payment(store.payAllRequest())
        } else if let sheet {
            router.sheet = sheet
        }
        if assistant { router.isAssistantPresented = true }
    }
}

extension Route {
    /// "topic:driving-and-transport", "service:renew-vehicle-registration", "bundle:pay-all",
    /// "journey:build-a-house", "request:sara-visa", "vehicle:sara-patrol", "allServices", "entities", …
    nonisolated init?(launchString s: String) {
        let parts = s.split(separator: ":", maxSplits: 1).map(String.init)
        let key = parts[0], value = parts.count > 1 ? parts[1] : ""
        switch key {
        case "topic": self = .topic(value)
        case "subtopic":
            let tv = value.split(separator: "/").map(String.init)
            guard tv.count == 2 else { return nil }
            self = .subtopic(topic: tv[0], sub: tv[1])
        case "service": self = .service(value)
        case "bundle": self = .bundle(value)
        case "journey": self = .journey(value)
        case "allServices": self = .allServices
        case "entities": self = .entities
        case "entity": self = .entity(value)
        case "bundlesList": self = .bundlesList
        case "journeysList": self = .journeysList
        case "vehicle": self = .vehicle(value)
        case "vehicles": self = .vehicles
        case "documents": self = .documents
        case "document": self = .document(value)
        case "family": self = .family
        case "request": self = .request(value)
        case "receipt": self = .receipt(value)
        case "drafts": self = .drafts
        case "history": self = .history
        case "notifications": self = .notifications
        default: return nil
        }
    }
}

extension SheetRoute {
    /// "profile", "happiness", "notifications", "widgetGallery", "editFavorites", "extendParking",
    /// "topUpSalik", "demoMenu", "payAll", "receipt:<id>", "comingSoon"
    nonisolated init?(launchString s: String) {
        let parts = s.split(separator: ":", maxSplits: 1).map(String.init)
        switch parts[0] {
        case "profile": self = .profile
        case "happiness": self = .happinessMeter(context: nil)
        case "notifications": self = .notifications
        case "widgetGallery": self = .widgetGallery
        case "editFavorites": self = .editFavorites
        case "extendParking": self = .extendParking
        case "topUpSalik": self = .topUpSalik
        case "demoMenu": self = .demoMenu
        case "receipt": self = .receipt(parts.count > 1 ? parts[1] : "")
        case "comingSoon": self = .comingSoon(ComingSoon(title: "Renew your vehicle registration", owner: "RTA", symbol: "car.fill", hue: .sky))
        case "payAll":
            // Resolved later by the store (needs the persona's payables).
            self = .payment(PaymentRequest(id: "launch-pay-all", title: "Pay all", lines: [], purpose: .payables([])))
        default: return nil
        }
    }
}
