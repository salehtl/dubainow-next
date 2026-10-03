import SwiftUI

/// Maps every `Route` to its feature view. Feature folders own the views; this file is the
/// contract between them, so type names here must not change.
struct RouteDestinationView: View {
    let route: Route

    var body: some View {
        switch route {
        case .topic(let id): TopicView(topicID: id)
        case .subtopic(let topic, let sub): SubtopicView(topicID: topic, subtopicID: sub)
        case .service(let id): ServiceStartView(serviceID: id)
        case .bundle(let id): BundleView(serviceID: id)
        case .journey(let id): JourneyView(serviceID: id)
        case .allServices: AllServicesView()
        case .entities: EntitiesView()
        case .entity(let name): EntityView(name: name)
        case .bundlesList: BundlesListView()
        case .journeysList: JourneysListView()
        case .vehicle(let id): VehicleView(vehicleID: id)
        case .vehicles: VehiclesView()
        case .documents: DocumentsView()
        case .document(let id): DocumentView(documentID: id)
        case .family: FamilyView()
        case .request(let id): RequestTrackerView(requestID: id)
        case .receipt(let id): ReceiptView(receiptID: id)
        case .drafts: DraftsView()
        case .history: HistoryView()
        case .notifications: NotificationsView()
        }
    }
}

extension View {
    /// Attach once at the root of each NavigationStack.
    func dnRouteDestinations() -> some View {
        navigationDestination(for: Route.self) { RouteDestinationView(route: $0) }
    }
}

/// Maps every `SheetRoute` to its sheet view.
struct SheetDestination: View {
    let sheet: SheetRoute

    var body: some View {
        content
            .environment(\.dnIsInSheet, true)
            .modifier(SheetToastHost(enabled: sheet != .profile))
    }

    @ViewBuilder private var content: some View {
        switch sheet {
        case .profile: ProfileSheet()
        case .happinessMeter(let context): HappinessMeterSheet(context: context)
        case .notifications: NavigationStack { NotificationsView().dnRouteDestinations() }
        case .payment(let request): PaymentSheet(request: request)
        case .receipt(let id): ReceiptSheet(receiptID: id)
        case .widgetGallery: WidgetGallerySheet()
        case .editFavorites: EditFavoritesSheet()
        case .comingSoon(let item): ComingSoonSheet(item: item)
        case .extendParking: ExtendParkingSheet()
        case .topUpSalik: TopUpSalikSheet()
        case .demoMenu: DemoMenuSheet()
        }
    }
}

/// Toasts raised while a sheet is open show inside the sheet (the root host sits behind it).
private struct SheetToastHost: ViewModifier {
    var enabled: Bool
    func body(content: Content) -> some View {
        if enabled { content.toastHost(bottomInset: 24) } else { content }
    }
}
