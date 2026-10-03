import SwiftUI

/// Every journey ("Start to finish"); journeys the resident started come first, with progress.
struct JourneysListView: View {
    @Environment(DemoStore.self) private var store
    @Environment(Router.self) private var router

    private var journeys: [CatalogueService] {
        let started = Set(store.state.journeys.map(\.journeyID))
        let all = Catalogue.shared.journeys
        return all.filter { started.contains($0.id) } + all.filter { !started.contains($0.id) }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 8) {
                    ServiceTypeBadge(type: .journey)
                    Text("Start to finish")
                        .dnFont(.title1)
                        .foregroundStyle(DNColor.textPrimary)
                        .accessibilityAddTraits(.isHeader)
                    Text("Big moments that need several entities over weeks or months. DubaiNow keeps the plan, does what it can and shows who handles the rest.")
                        .dnFont(.callout)
                        .foregroundStyle(DNColor.textSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .padding(.bottom, 8)
                ForEach(journeys) { journey in
                    Button {
                        router.open(service: journey)
                    } label: {
                        JourneyCard(service: journey, progress: store.servicesJourney(journey.id), width: nil)
                    }
                    .buttonStyle(.dnPressable)
                }
            }
            .padding(.horizontal, DNSpace.insetPage)
            .padding(.top, 8)
            .padding(.bottom, 32)
        }
        .dnCanvasBackground()
        .navigationTitle("Start to finish")
        .navigationBarTitleDisplayMode(.inline)
        .dnPushedToolbar()
    }
}
