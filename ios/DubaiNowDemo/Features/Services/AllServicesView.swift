import SwiftUI

/// All services, A to Z: every one of the catalogue's records in a native list with sticky
/// letter sections and a section index, a type filter, and its own search field.
struct AllServicesView: View {
    @Environment(Router.self) private var router
    @State private var filter: ServicesData.Filter = .all
    @State private var query = ""

    private var services: [CatalogueService] {
        let list = ServicesData.aToZ(filter)
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !q.isEmpty else { return list }
        return list.filter { s in
            s.name.localizedCaseInsensitiveContains(q)
                || s.aliases.contains { $0.localizedCaseInsensitiveContains(q) }
                || (s.owner ?? "").localizedCaseInsensitiveContains(q)
        }
    }

    var body: some View {
        let list = services
        let sections = ServicesData.letterSections(list)
        ScrollViewReader { proxy in
            List {
                Section {
                    Picker("Type", selection: $filter) {
                        ForEach(ServicesData.Filter.allCases) { Text($0.label).tag($0) }
                    }
                    .pickerStyle(.segmented)
                    .listRowInsets(EdgeInsets(top: 4, leading: DNSpace.insetPage, bottom: 8, trailing: DNSpace.insetPage))
                    .listRowSeparator(.hidden)
                    .listRowBackground(Color.clear)
                    Text(summary(count: list.count))
                        .dnFont(.footnote)
                        .foregroundStyle(DNColor.textSecondary)
                        .listRowInsets(EdgeInsets(top: 0, leading: DNSpace.insetPage, bottom: 8, trailing: DNSpace.insetPage))
                        .listRowSeparator(.hidden)
                        .listRowBackground(Color.clear)
                }
                ForEach(sections) { section in
                    Section {
                        ForEach(section.services) { s in
                            ServicesRow(service: s, trailing: .chevron)
                                // Leave room on the right for the section index.
                                .listRowInsets(EdgeInsets(top: 0, leading: 0, bottom: 0, trailing: 12))
                                .alignmentGuide(.listRowSeparatorLeading) { _ in 68 }
                        }
                    } header: {
                        Text(section.letter)
                            .dnFont(.headline)
                            .foregroundStyle(DNColor.textPrimary)
                            .accessibilityLabel("Letter \(section.letter)")
                    }
                    .sectionIndexLabel(section.letter)
                    .id(section.letter)
                }
            }
            .listStyle(.plain)
            .servicesLaunchScroll(proxy)
        }
        .listSectionIndexVisibility(.visible)
        .overlay {
            if list.isEmpty {
                ContentUnavailableView.search(text: query)
            }
        }
        .searchable(text: $query, placement: .navigationBarDrawer(displayMode: .always), prompt: "Search \(ServicesText.count(ServicesData.aToZ(filter).count, "service"))")
        .animation(DNMotion.resolve(DNMotion.fast), value: filter)
        .onChange(of: filter) { DNHaptics.selection() }
        .navigationTitle("All services, A to Z")
        .navigationBarTitleDisplayMode(.inline)
        .dnPushedToolbar()
    }

    private func summary(count: Int) -> String {
        switch filter {
        case .all: "\(ServicesText.count(count, "service")): standalone services, bundles and journeys."
        case .standalone: "\(ServicesText.count(count, "standalone service")). One task with one entity."
        case .bundle: "\(ServicesText.count(count, "bundle")). The same job across many accounts, done in one go."
        case .journey: "\(ServicesText.count(count, "journey")). Life events that need several services, in order."
        }
    }
}
