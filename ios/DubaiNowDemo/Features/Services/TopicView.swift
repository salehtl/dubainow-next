import SwiftUI

/// A topic page (Paper C1 `DJ8-0`): hue disc header, sub-topic chips that scroll to their
/// section, a personal banner, up to 3 rows per sub-topic with "See all N", then the
/// topic's bundles and journeys.
struct TopicView: View {
    var topicID: String

    var body: some View {
        if let topic = Catalogue.shared.topic(topicID) {
            ServicesTopicPage(topic: topic)
        } else {
            ServicesMissingRecord(id: topicID)
        }
    }
}

private struct ServicesTopicPage: View {
    var topic: CatalogueTopic

    @Environment(DemoStore.self) private var store
    @Environment(Router.self) private var router

    private static let multiSectionID = "section-multi"

    var body: some View {
        let subtopics = ServicesData.subtopics(of: topic)
        let multi = ServicesData.bundlesAndJourneys(inTopic: topic.id)
        let unfiled = ServicesData.unfiled(inTopic: topic)

        ScrollViewReader { proxy in
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    header
                        .padding(.horizontal, DNSpace.insetPage)
                    chips(subtopics, hasMulti: !multi.isEmpty, proxy: proxy)
                        .padding(.top, 16)
                    if let banner = store.servicesBanner(forTopic: topic.id) {
                        ServicesBannerCard(banner: banner)
                            .padding(.horizontal, DNSpace.insetPage)
                            .padding(.top, 20)
                    }
                    VStack(alignment: .leading, spacing: 24) {
                        ForEach(subtopics) { sub in
                            section(sub)
                        }
                        if !unfiled.isEmpty {
                            VStack(alignment: .leading, spacing: 8) {
                                ServicesSectionHeader(title: "Other services")
                                ServicesRowGroup {
                                    ForEach(unfiled) { ServicesRow(service: $0, showsIcon: false) }
                                }
                            }
                        }
                        if !multi.isEmpty {
                            multiSection(multi)
                                .id(Self.multiSectionID)
                        }
                    }
                    .padding(.horizontal, DNSpace.insetPage)
                    .padding(.top, 24)
                }
                .padding(.top, 8)
                .padding(.bottom, 32)
            }
            .servicesLaunchScroll(proxy)
        }
        .background(ServicesHueWash(color: topic.hue.start.opacity(0.9)))
        .navigationTitle(topic.name)
        .navigationBarTitleDisplayMode(.inline)
        .dnPushedToolbar()
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                ShareLink(item: "\(topic.name) on DubaiNow: \(ServicesData.topicLine(topic)).") {
                    Image(systemName: "square.and.arrow.up")
                }
                .accessibilityLabel("Share")
            }
        }
    }

    // MARK: Header

    private var header: some View {
        VStack(alignment: .leading, spacing: 12) {
            ServicesHeaderDisc(symbol: topic.symbol, hue: topic.hue, size: 56)
            VStack(alignment: .leading, spacing: 6) {
                Text(topic.name)
                    .dnFont(.title1)
                    .foregroundStyle(DNColor.textPrimary)
                    .accessibilityAddTraits(.isHeader)
                Text(ServicesData.topicLine(topic))
                    .dnFont(.subheadline)
                    .foregroundStyle(DNColor.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }

    // MARK: Chips

    private func chips(_ subtopics: [CatalogueSubtopic], hasMulti: Bool, proxy: ScrollViewProxy) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                // Chips are identified by position so `scrollTo(sub.id)` lands on the section.
                ForEach(Array(subtopics.enumerated()), id: \.offset) { _, sub in
                    chip(sub.name, count: sub.services) { jump(to: sub.id, proxy: proxy) }
                }
                if hasMulti {
                    chip("All at once and start to finish", count: nil) { jump(to: Self.multiSectionID, proxy: proxy) }
                }
            }
        }
        .contentMargins(.horizontal, DNSpace.insetPage, for: .scrollContent)
        .scrollClipDisabled()
    }

    private func chip(_ title: String, count: Int?, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Text(title)
                    .dnFont(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundStyle(DNColor.textPrimary)
                if let count {
                    Text("\(count)")
                        .dnFont(.footnote)
                        .fontWeight(.semibold)
                        .foregroundStyle(DNColor.textSecondary)
                }
            }
            .padding(.horizontal, 14)
            .frame(minHeight: 40)
            .background(DNColor.surfacePrimary, in: .capsule)
            .contentShape(.capsule)
        }
        .buttonStyle(.dnPressable)
        .accessibilityLabel(count.map { "\(title), \($0) services" } ?? title)
        .accessibilityHint("Scrolls to the section")
    }

    private func jump(to id: String, proxy: ScrollViewProxy) {
        DNHaptics.selection()
        withAnimation(DNMotion.resolve(DNMotion.smooth)) { proxy.scrollTo(id, anchor: .top) }
    }

    // MARK: Sections

    private func section(_ sub: CatalogueSubtopic) -> some View {
        let all = ServicesData.services(topic: topic.id, subtopic: sub.id)
        let rows = ServicesData.preview(topic: topic.id, subtopic: sub.id)
        return VStack(alignment: .leading, spacing: 8) {
            ServicesSectionHeader(
                title: sub.name,
                linkTitle: all.count > rows.count ? "See all \(all.count)" : nil,
                action: all.count > rows.count ? { router.push(.subtopic(topic: topic.id, sub: sub.id)) } : nil
            )
            ServicesRowGroup {
                ForEach(rows) { ServicesRow(service: $0, showsIcon: false) }
            }
        }
    }

    private func multiSection(_ multi: [CatalogueService]) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            ServicesSectionHeader(
                title: multi.allSatisfy { $0.type == .journey } ? "Start to finish"
                    : multi.allSatisfy { $0.type == .bundle } ? "All at once" : "All at once and start to finish",
                subtitle: "Several services, handled together"
            )
            ForEach(multi) { s in
                Button {
                    router.open(service: s)
                } label: {
                    if s.type == .journey {
                        JourneyCard(service: s, progress: store.servicesJourney(s.id), width: nil)
                    } else {
                        BundleCard(service: s, count: store.servicesBundleCount(s), width: nil)
                    }
                }
                .buttonStyle(.dnPressable)
            }
        }
    }
}
