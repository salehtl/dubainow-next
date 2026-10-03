import SwiftUI

/// Services tab root (Paper C1 `DBS-0`): For you, Browse by topic, All at once (bundles),
/// Start to finish (journeys) and Browse another way (A to Z, by entity).
/// Numbers come from the refactored catalogue, not the older Paper copy.
struct ServicesView: View {
    @Environment(DemoStore.self) private var store
    @Environment(Router.self) private var router
    @Environment(\.dynamicTypeSize) private var dynamicType

    private let catalogue = Catalogue.shared

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView {
                VStack(alignment: .leading, spacing: 28) {
                    header
                    if !store.isGuest, !store.servicesForYou.isEmpty { forYou }
                    topics.id("topics")
                    bundles.id("bundles")
                    journeys.id("journeys")
                    browseAnotherWay.id("browse")
                }
                .padding(.top, 4)
                .padding(.bottom, 32)
            }
            .servicesLaunchScroll(proxy)
        }
        .dnCanvasBackground()
        .navigationBarTitleDisplayMode(.inline)
        .dnMainToolbar()
        .onAppear { ServicesReachability.runIfRequested() }
    }

    // MARK: Header

    private var header: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Services")
                .dnFont(.largeTitle)
                .foregroundStyle(DNColor.textPrimary)
                .accessibilityAddTraits(.isHeader)
            Text("\(catalogue.services.count) services from \(ServicesData.entityCount) government entities and providers, organised by what you need to do.")
                .dnFont(.subheadline)
                .foregroundStyle(DNColor.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(.horizontal, DNSpace.insetPage)
    }

    // MARK: For you

    private var forYou: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .firstTextBaseline) {
                Text("For you")
                    .dnFont(.section)
                    .foregroundStyle(DNColor.textPrimary)
                    .accessibilityAddTraits(.isHeader)
                Spacer()
                Text("from your profile")
                    .dnFont(.subheadline)
                    .foregroundStyle(DNColor.textSecondary)
            }
            .padding(.horizontal, DNSpace.insetPage)

            ServicesShelf {
                ForEach(store.servicesForYou) { item in
                    Button {
                        router.servicesPerform(item.action)
                    } label: {
                        ServicesForYouCard(item: item, width: dynamicType.isAccessibilitySize ? 300 : 248)
                    }
                    .buttonStyle(.dnPressable)
                }
            }
        }
    }

    // MARK: Browse by topic

    private var topics: some View {
        let columns = dynamicType.isAccessibilitySize
            ? [GridItem(.flexible())]
            : [GridItem(.flexible(), spacing: DNSpace.widgetGutter), GridItem(.flexible())]
        return VStack(alignment: .leading, spacing: 12) {
            Text("Browse by topic")
                .dnFont(.section)
                .foregroundStyle(DNColor.textPrimary)
                .accessibilityAddTraits(.isHeader)
            LazyVGrid(columns: columns, spacing: DNSpace.widgetGutter) {
                ForEach(catalogue.topics) { topic in
                    Button {
                        router.push(.topic(topic.id))
                    } label: {
                        ServicesTopicTile(topic: topic, compact: !dynamicType.isAccessibilitySize)
                    }
                    .buttonStyle(.dnPressable)
                    .accessibilityHint("Opens the topic")
                }
            }
        }
        .padding(.horizontal, DNSpace.insetPage)
    }

    // MARK: All at once

    private var bundles: some View {
        VStack(alignment: .leading, spacing: 12) {
            ServicesSectionHeader(
                title: "All at once",
                subtitle: "One job across all your accounts, done in one go",
                linkTitle: "See all",
                action: { router.push(.bundlesList) }
            )
            .padding(.horizontal, DNSpace.insetPage)

            ServicesShelf {
                ForEach(orderedBundles) { bundle in
                    Button {
                        router.open(service: bundle)
                    } label: {
                        BundleCard(service: bundle, count: store.servicesBundleCount(bundle))
                    }
                    .buttonStyle(.dnPressable)
                }
            }
        }
    }

    /// Bundles with something personal to say (Pay all: 2 bills; Family visas: 2 due) come first.
    private var orderedBundles: [CatalogueService] {
        let all = catalogue.bundles
        return all.filter { store.servicesBundleCount($0) != nil } + all.filter { store.servicesBundleCount($0) == nil }
    }

    // MARK: Start to finish

    /// Journeys the resident started come first.
    private var orderedJourneys: [CatalogueService] {
        let started = Set(store.state.journeys.map(\.journeyID))
        return catalogue.journeys.filter { started.contains($0.id) } + catalogue.journeys.filter { !started.contains($0.id) }
    }

    private var journeys: some View {
        VStack(alignment: .leading, spacing: 12) {
            ServicesSectionHeader(
                title: "Start to finish",
                subtitle: "Life events that need several services, in order",
                linkTitle: "See all",
                action: { router.push(.journeysList) }
            )
            .padding(.horizontal, DNSpace.insetPage)

            ServicesShelf {
                ForEach(orderedJourneys) { journey in
                    Button {
                        router.open(service: journey)
                    } label: {
                        JourneyCard(service: journey, progress: store.servicesJourney(journey.id))
                    }
                    .buttonStyle(.dnPressable)
                }
            }
        }
    }

    // MARK: Browse another way

    private var browseAnotherWay: some View {
        DNListGroup(header: "Browse another way") {
            Button { router.push(.allServices) } label: {
                DNListRow(title: "All services, A to Z", value: "\(catalogue.services.count)")
            }
            .buttonStyle(ServicesRowButtonStyle())
            Button { router.push(.entities) } label: {
                DNListRow(title: "By government entity", value: "\(ServicesData.entityCount)")
            }
            .buttonStyle(ServicesRowButtonStyle())
        }
        .padding(.horizontal, DNSpace.insetPage)
    }
}
