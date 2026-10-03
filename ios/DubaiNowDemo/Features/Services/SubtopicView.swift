import SwiftUI

/// Every record in one sub-topic ("See all N" from a topic page): standalone services most
/// used first, then any bundle or journey that lives here.
struct SubtopicView: View {
    var topicID: String; var subtopicID: String

    @Environment(Router.self) private var router

    var body: some View {
        if let topic = Catalogue.shared.topic(topicID), let sub = topic.subtopics.first(where: { $0.id == subtopicID }) {
            let services = ServicesData.services(topic: topicID, subtopic: subtopicID)
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    VStack(alignment: .leading, spacing: 12) {
                        ServicesHeaderDisc(symbol: ServiceVisuals.symbol(topic: topicID, subtopic: subtopicID), hue: topic.hue, size: 56)
                        Button {
                            router.push(.topic(topicID))
                        } label: {
                            Text(topic.name)
                                .dnFont(.footnote)
                                .fontWeight(.medium)
                                .foregroundStyle(DNColor.textLink)
                                .frame(minHeight: 32)
                        }
                        .buttonStyle(.plain)
                        .accessibilityHint("Opens the topic")
                        VStack(alignment: .leading, spacing: 4) {
                            Text(sub.name)
                                .dnFont(.title1)
                                .foregroundStyle(DNColor.textPrimary)
                                .accessibilityAddTraits(.isHeader)
                            Text(ServicesText.count(services.count, "service"))
                                .dnFont(.subheadline)
                                .foregroundStyle(DNColor.textSecondary)
                        }
                    }
                    // Rows in one sub-topic share a glyph, so only bundles and journeys show theirs.
                    ServicesRowGroup {
                        ForEach(services) { ServicesRow(service: $0, showsIcon: $0.type != .standalone) }
                    }
                }
                .padding(.horizontal, DNSpace.insetPage)
                .padding(.top, 8)
                .padding(.bottom, 32)
            }
            .background(ServicesHueWash(color: topic.hue.start.opacity(0.9)))
            .navigationTitle(sub.name)
            .navigationBarTitleDisplayMode(.inline)
            .dnPushedToolbar()
        } else {
            ServicesMissingRecord(id: "\(topicID)/\(subtopicID)")
        }
    }
}
