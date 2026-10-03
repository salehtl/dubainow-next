import SwiftUI

/// Debug screen showing the shared components (launch with `-componentGallery`).
struct ComponentGallery: View {
    @Environment(DemoStore.self) private var store
    @State private var face: HappinessFace? = .happy

    var body: some View {
        let cat = Catalogue.shared
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                SectionHeader(title: "Needs you") { DNTextButton(title: "See all") {} }
                HStack(spacing: 12) {
                    HueTile(symbol: "p.square.fill", hue: .sky)
                    HueTile(symbol: "road.lanes", hue: .sand)
                    HueTile(symbol: "car.fill", hue: .rose)
                    HueTile(symbol: "heart.fill", hue: .lilac)
                }
                HStack(spacing: 12) {
                    ForEach(Hue.allCases, id: \.self) { HueIcon(symbol: "bolt.fill", hue: $0, size: 44) }
                }
                HStack {
                    StatusChip(text: "Due tomorrow", tone: .warning)
                    StatusChip(text: "In review", tone: .info, onWhite: true)
                    NumberPlate(code: "M", number: "48213")
                }
                MoneyText(amount: 412.60)
                SegmentedProgress(total: 5, done: 2)
                Button("Pay all · AED 1,012.60") {}.buttonStyle(.dn(.primary, size: .medium, fullWidth: false))
                HStack {
                    Button("Pay now") {}.buttonStyle(.dn(.primary, size: .medium))
                    Button("Later") {}.buttonStyle(.dn(.secondary, size: .medium, fullWidth: false))
                }
                DNListGroup(header: "Services") {
                    if let s = cat.service("renew-vehicle-registration") { CatalogueRow(service: s, trailing: .favorite, context: "due in 12 days", contextTone: .warning) }
                    if let s = cat.service("pay-all") { CatalogueRow(service: s) }
                    if let s = cat.service("build-a-house") { CatalogueRow(service: s) }
                    if let s = cat.service("top-up-salik-account") { CatalogueRow(service: s, highlight: "sal") }
                }
                ScrollView(.horizontal) {
                    HStack(spacing: 12) {
                        if let s = cat.service("pay-all") { BundleCard(service: s, count: 2) }
                        if let s = cat.service("move-home") { JourneyCard(service: s, progress: store.state.journeys.first) }
                    }
                    .padding(.vertical, 20)
                }
                HappinessFacesPicker(selection: $face)
                HStack { DNLogo(height: 40); Avatar(initials: "S", hue: .peach, size: 44); GlassIconButton(symbol: "xmark", label: "Close") {} }
            }
            .padding(20)
        }
        .background(alignment: .top) { AmbientWash(timeOfDay: store.timeOfDay) }
        .dnCanvasBackground()
    }
}
