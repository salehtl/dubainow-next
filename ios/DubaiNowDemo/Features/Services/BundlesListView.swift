import SwiftUI

/// Every bundle ("All at once"), with the resident's own count line where we know it.
struct BundlesListView: View {
    @Environment(DemoStore.self) private var store
    @Environment(Router.self) private var router

    private var bundles: [CatalogueService] {
        let all = Catalogue.shared.bundles
        return all.filter { store.servicesBundleCount($0) != nil } + all.filter { store.servicesBundleCount($0) == nil }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 8) {
                    ServiceTypeBadge(type: .bundle)
                    Text("All at once")
                        .dnFont(.title1)
                        .foregroundStyle(DNColor.textPrimary)
                        .accessibilityAddTraits(.isHeader)
                    Text("The same job across many accounts, people or entities, done in one go: one set of questions, one payment, one receipt. Each part still works on its own.")
                        .dnFont(.callout)
                        .foregroundStyle(DNColor.textSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .padding(.bottom, 8)
                ForEach(bundles) { bundle in
                    Button {
                        router.open(service: bundle)
                    } label: {
                        BundleCard(service: bundle, count: store.servicesBundleCount(bundle), width: nil)
                    }
                    .buttonStyle(.dnPressable)
                }
            }
            .padding(.horizontal, DNSpace.insetPage)
            .padding(.top, 8)
            .padding(.bottom, 32)
        }
        .dnCanvasBackground()
        .navigationTitle("All at once")
        .navigationBarTitleDisplayMode(.inline)
        .dnPushedToolbar()
    }
}
