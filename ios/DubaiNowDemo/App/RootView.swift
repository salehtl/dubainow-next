import SwiftUI

/// Welcome (signed out) or the main app. The assistant overlay and toasts sit above both.
struct RootView: View {
    @Environment(DemoStore.self) private var store
    @Environment(Router.self) private var router

    var body: some View {
        ZStack {
            if ProcessInfo.processInfo.arguments.contains("-componentGallery") {
                ComponentGallery()
            } else if store.session == nil {
                WelcomeView()
                    .transition(.opacity)
            } else {
                MainTabView()
                    .transition(.opacity)
            }

            if router.isAssistantPresented, store.session != nil {
                AssistantOverlay()
                    .transition(.opacity)
                    .zIndex(10)
            }
        }
        .toastHost()
        .animation(DNMotion.resolve(DNMotion.smooth), value: store.session == nil)
        .onChange(of: store.session) { _, new in
            if new == nil { router.reset() }
        }
    }
}

/// iOS's own tab bar: Home, Services, Activity, plus the search tab (a separate glass circle).
/// Each tab has its own NavigationStack; sheets are presented from here.
struct MainTabView: View {
    @Environment(DemoStore.self) private var store
    @Environment(Router.self) private var router

    var body: some View {
        @Bindable var router = router
        TabView(selection: $router.tab) {
            Tab("Home", systemImage: "house.fill", value: AppTab.home) {
                NavigationStack(path: $router.homePath) {
                    HomeView().dnRouteDestinations()
                        .toolbarVisibility(router.isAssistantPresented ? .hidden : .automatic, for: .tabBar)
                }
            }
            Tab("Services", systemImage: "square.grid.2x2.fill", value: AppTab.services) {
                NavigationStack(path: $router.servicesPath) {
                    ServicesView().dnRouteDestinations()
                        .toolbarVisibility(router.isAssistantPresented ? .hidden : .automatic, for: .tabBar)
                }
            }
            Tab("Activity", systemImage: "clock.arrow.circlepath", value: AppTab.activity) {
                NavigationStack(path: $router.activityPath) {
                    ActivityView().dnRouteDestinations()
                        .toolbarVisibility(router.isAssistantPresented ? .hidden : .automatic, for: .tabBar)
                }
            }
            Tab(value: AppTab.search, role: .search) {
                NavigationStack(path: $router.searchPath) {
                    SearchView().dnRouteDestinations()
                        .toolbarVisibility(router.isAssistantPresented ? .hidden : .automatic, for: .tabBar)
                }
                .searchable(text: $router.searchQuery, prompt: "Services, fines, \"how much do I owe\"")
            }
        }
        .tabViewSearchActivation(.searchTabSelection)
        .tabBarMinimizeBehavior(.never)
        .sheet(item: $router.sheet) { sheet in
            SheetDestination(sheet: sheet)
                .environment(store)
                .environment(router)
        }
        .onChange(of: router.tab) { _, _ in DNHaptics.selection() }
    }
}
