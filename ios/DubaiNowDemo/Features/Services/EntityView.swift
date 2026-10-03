import SwiftUI

/// One owner's services, grouped by topic.
struct EntityView: View {
    var name: String

    private var label: String { name == "To confirm" ? "Owner to confirm" : name }

    private var groups: [(title: String, services: [CatalogueService])] {
        let services = ServicesData.services(ownedBy: name)
        var order: [String] = []
        var byTopic: [String: [CatalogueService]] = [:]
        for s in services {
            let key = s.topic ?? ""
            if byTopic[key] == nil { order.append(key) }
            byTopic[key, default: []].append(s)
        }
        let topics = Catalogue.shared.topics.map(\.id)
        order.sort { (topics.firstIndex(of: $0) ?? .max) < (topics.firstIndex(of: $1) ?? .max) }
        return order.map { key in
            (Catalogue.shared.topic(key)?.name ?? "Across topics", (byTopic[key] ?? []).sorted { $0.name < $1.name })
        }
    }

    var body: some View {
        let total = ServicesData.services(ownedBy: name).count
        List {
            Section {
                HStack(spacing: 14) {
                    ServicesEntityMark(name: label, size: 52, fill: DNColor.surfacePrimary)
                    VStack(alignment: .leading, spacing: 4) {
                        Text(label)
                            .dnFont(.title2)
                            .foregroundStyle(DNColor.textPrimary)
                            .accessibilityAddTraits(.isHeader)
                        Text(subtitle(total: total))
                            .dnFont(.subheadline)
                            .foregroundStyle(DNColor.textSecondary)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets(top: 8, leading: 4, bottom: 8, trailing: 4))
            }
            ForEach(groups, id: \.title) { group in
                Section(group.title) {
                    ForEach(group.services) { s in
                        ServicesRow(service: s, trailing: .favorite)
                            .listRowInsets(EdgeInsets())
                            .alignmentGuide(.listRowSeparatorLeading) { _ in 68 }
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .scrollContentBackground(.hidden)
        .dnCanvasBackground()
        .overlay {
            if total == 0 {
                ContentUnavailableView("No services", systemImage: "building.columns", description: Text("\(label) doesn't run any services in DubaiNow yet."))
            }
        }
        .navigationTitle(label)
        .navigationBarTitleDisplayMode(.inline)
        .dnPushedToolbar()
    }

    private func subtitle(total: Int) -> String {
        let full = ServicesData.fullName(name)
        let count = ServicesText.count(total, "service")
        switch name {
        case "To confirm": return "\(count) whose owner is being checked with government"
        case "DubaiNow": return "\(count): bundles, journeys and DubaiNow's own"
        default: return full == name ? count : "\(full) · \(count)"
        }
    }
}
