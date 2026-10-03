import SwiftUI

/// By government entity: every owner with its service count (entities are a filter, not a
/// structure — this list is for people who think that way).
struct EntitiesView: View {
    @Environment(Router.self) private var router
    @State private var query = ""

    private var owners: [ServicesData.Owner] {
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !q.isEmpty else { return ServicesData.owners }
        return ServicesData.owners.filter {
            $0.label.localizedCaseInsensitiveContains(q) || ($0.detail ?? "").localizedCaseInsensitiveContains(q)
        }
    }

    var body: some View {
        List {
            Section {
                ForEach(owners) { owner in
                    Button {
                        router.push(.entity(owner.key))
                    } label: {
                        HStack(spacing: 12) {
                            ServicesEntityMark(name: owner.label)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(owner.label)
                                    .dnFont(.rowTitle)
                                    .foregroundStyle(DNColor.listRowTitle)
                                if let detail = owner.detail {
                                    Text(detail)
                                        .dnFont(.footnote)
                                        .foregroundStyle(DNColor.listRowSupporting)
                                }
                            }
                            Spacer(minLength: 8)
                            Text("\(owner.count)")
                                .dnFont(.subheadline)
                                .fontWeight(.semibold)
                                .fontDesign(.rounded)
                                .foregroundStyle(DNColor.textSecondary)
                            Image(systemName: "chevron.forward")
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundStyle(DNColor.listRowChevron)
                                .accessibilityHidden(true)
                        }
                        .frame(minHeight: 44)
                        .contentShape(.rect)
                    }
                    .buttonStyle(.plain)
                    .accessibilityElement(children: .combine)
                    .accessibilityLabel("\(owner.label), \(ServicesText.count(owner.count, "service"))")
                }
            } header: {
                Text("\(ServicesData.entityCount) government entities and providers")
            } footer: {
                Text("Each service is listed under the entity that runs it. Bundles and journeys are run by DubaiNow with several entities.")
            }
        }
        .listStyle(.insetGrouped)
        .scrollContentBackground(.hidden)
        .dnCanvasBackground()
        .overlay {
            if owners.isEmpty { ContentUnavailableView.search(text: query) }
        }
        .searchable(text: $query, placement: .navigationBarDrawer(displayMode: .automatic), prompt: "Search entities")
        .navigationTitle("By government entity")
        .navigationBarTitleDisplayMode(.inline)
        .dnPushedToolbar()
    }
}

/// A monogram disc for an entity (no logos in the demo).
struct ServicesEntityMark: View {
    var name: String
    var size: CGFloat = 36
    var fill: Color = DNColor.surfaceSecondary

    var body: some View {
        let initials = Self.initials(name)
        Circle()
            .fill(fill)
            .overlay(
                Text(initials.isEmpty ? String(name.prefix(1)).uppercased() : initials)
                    .font(.system(size: size * 0.36, weight: .bold, design: .rounded))
                    .foregroundStyle(DNColor.textSecondary)
                    .minimumScaleFactor(0.6)
            )
            .frame(width: size, height: size)
            .accessibilityHidden(true)
    }

    /// "RTA" stays "RTA"; "Awqaf and Minors Affairs Foundation" → "AM"; "Owner to confirm" → "?".
    static func initials(_ name: String) -> String {
        if name == "Owner to confirm" { return "?" }
        if name.count <= 5, !name.contains(" ") { return name }
        if !name.contains(" ") {
            let capitals = name.filter(\.isUppercase)
            return capitals.count >= 2 ? String(capitals.prefix(2)) : String(name.prefix(1)).uppercased()
        }
        let words = name.split(separator: " ").filter { !["and", "of", "in", "the", "or"].contains($0.lowercased()) }
        return words.prefix(2).compactMap { $0.first.map { String($0).uppercased() } }.joined()
    }
}
