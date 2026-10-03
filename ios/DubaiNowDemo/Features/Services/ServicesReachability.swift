import Foundation

/// A quick check that every one of the catalogue's records can be reached from Services,
/// using the same lists the screens show. Launch with `-servicesCheck` and read the console (or tmp/services-check.txt in the app's data container):
///   xcrun simctl launch --console-pty <udid> com.salehtl.DubaiNowNext -persona sara -tab services -servicesCheck
enum ServicesReachability {
    struct Report {
        var total: Int
        var viaTopics: Set<String>
        var viaShelves: Set<String>
        var viaAToZ: Set<String>
        var viaEntities: Set<String>
        var viaLists: Set<String>

        var reachable: Set<String> { viaTopics.union(viaShelves).union(viaAToZ).union(viaEntities).union(viaLists) }
        var unreachable: [String] {
            Catalogue.shared.services.map(\.id).filter { !reachable.contains($0) }
        }
    }

    static func report() -> Report {
        let cat = Catalogue.shared
        // Services → topic → sub-topic sections and "See all" (SubtopicView), plus the topic's
        // bundles and journeys section and any unfiled services.
        var topics = Set<String>()
        for topic in cat.topics {
            for sub in ServicesData.subtopics(of: topic) {
                topics.formUnion(ServicesData.services(topic: topic.id, subtopic: sub.id).map(\.id))
            }
            topics.formUnion(ServicesData.bundlesAndJourneys(inTopic: topic.id).map(\.id))
            topics.formUnion(ServicesData.unfiled(inTopic: topic).map(\.id))
        }
        // Services → All at once / Start to finish shelves.
        let shelves = Set(cat.bundles.map(\.id) + cat.journeys.map(\.id))
        // Services → All services, A to Z (the "All" filter), and each filter together.
        var aToZ = Set(ServicesData.aToZ(.all).map(\.id))
        let filtered = ServicesData.aToZ(.standalone) + ServicesData.aToZ(.bundle) + ServicesData.aToZ(.journey)
        assert(filtered.count == aToZ.count, "A to Z filters should partition the catalogue")
        aToZ.formIntersection(Set(filtered.map(\.id)))
        // Services → By government entity → entity.
        var entities = Set<String>()
        for owner in ServicesData.owners { entities.formUnion(ServicesData.services(ownedBy: owner.key).map(\.id)) }
        // See all → bundles list / journeys list.
        let lists = Set(cat.bundles.map(\.id) + cat.journeys.map(\.id))

        return Report(total: cat.services.count, viaTopics: topics, viaShelves: shelves, viaAToZ: aToZ, viaEntities: entities, viaLists: lists)
    }

    static func summary() -> String {
        let r = report()
        let cat = Catalogue.shared
        let topicOnlyMissing = cat.services.map(\.id).filter { !r.viaTopics.contains($0) && !r.viaShelves.contains($0) }
        let topicSum = cat.topics.reduce(0) { $0 + $1.services }
        return """
        [ServicesCheck] catalogue: \(r.total) services (\(cat.services.filter { $0.type == .standalone }.count) standalone, \(cat.bundles.count) bundles, \(cat.journeys.count) journeys)
        [ServicesCheck] reachable: \(r.reachable.count) of \(r.total) · unreachable: \(r.unreachable.isEmpty ? "none" : r.unreachable.joined(separator: ", "))
        [ServicesCheck] via topics: \(r.viaTopics.count) (topic counts add up to \(topicSum)) · via shelves: \(r.viaShelves.count) · via A to Z: \(r.viaAToZ.count) · via entities: \(r.viaEntities.count) (\(ServicesData.owners.count) rows)
        [ServicesCheck] not under a topic (reached from shelves, A to Z or entities instead): \(topicOnlyMissing.isEmpty ? "none" : topicOnlyMissing.joined(separator: ", "))
        """
    }

    /// Prints the summary once when the app is launched with `-servicesCheck`.
    static func runIfRequested() {
        guard !hasRun, ProcessInfo.processInfo.arguments.contains("-servicesCheck") else { return }
        hasRun = true
        let text = summary()
        print(text)
        // Also written to the app's tmp folder (read it with `simctl get_app_container … data`).
        try? text.write(to: FileManager.default.temporaryDirectory.appendingPathComponent("services-check.txt"), atomically: true, encoding: .utf8)
    }

    private static var hasRun = false
}
