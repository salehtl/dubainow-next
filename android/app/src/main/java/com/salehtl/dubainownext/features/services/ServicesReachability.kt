package com.salehtl.dubainownext.features.services

import android.content.Context
import android.util.Log
import com.salehtl.dubainownext.data.Catalogue
import com.salehtl.dubainownext.data.ServiceType
import java.io.File

/**
 * A quick check that every one of the catalogue's records can be reached from Services, using
 * the same lists the screens show. Launch with the extra `servicesCheck true` and read logcat
 * (tag ServicesCheck) or files/services-check.txt in the app's data folder:
 *   adb logcat -s ServicesCheck
 */
internal object ServicesReachability {
    data class Report(
        val total: Int,
        val viaTopics: Set<String>,
        val viaShelves: Set<String>,
        val viaAToZ: Set<String>,
        val viaEntities: Set<String>,
        val viaLists: Set<String>,
    ) {
        val reachable: Set<String> get() = viaTopics + viaShelves + viaAToZ + viaEntities + viaLists
        val unreachable: List<String> get() = Catalogue.shared.services.map { it.id }.filter { it !in reachable }
    }

    fun report(): Report {
        val cat = Catalogue.shared
        // Services → topic → sub-topic sections and "See all" (SubtopicScreen), plus the topic's
        // bundles and journeys section and any unfiled services.
        val topics = mutableSetOf<String>()
        for (topic in cat.topics) {
            for (sub in ServicesData.subtopics(topic)) topics += ServicesData.services(topic.id, sub.id).map { it.id }
            topics += ServicesData.bundlesAndJourneys(topic.id).map { it.id }
            topics += ServicesData.unfiled(topic).map { it.id }
        }
        // Services → All at once / Start to finish shelves.
        val shelves = (cat.bundles + cat.journeys).map { it.id }.toSet()
        // Services → All services, A to Z (the "All" filter), intersected with the three filters together.
        val all = ServicesData.aToZ(ServicesData.Filter.All).map { it.id }.toSet()
        val filtered = ServicesData.aToZ(ServicesData.Filter.Standalone) + ServicesData.aToZ(ServicesData.Filter.Bundle) +
            ServicesData.aToZ(ServicesData.Filter.Journey)
        check(filtered.size == all.size) { "A to Z filters should partition the catalogue" }
        val aToZ = all intersect filtered.map { it.id }.toSet()
        // Services → By government entity → entity.
        val entities = ServicesData.owners.flatMap { o -> ServicesData.services(ownedBy = o.key).map { it.id } }.toSet()
        // See all → bundles list / journeys list.
        val lists = (cat.bundles + cat.journeys).map { it.id }.toSet()
        return Report(cat.services.size, topics, shelves, aToZ, entities, lists)
    }

    fun summary(): String {
        val r = report()
        val cat = Catalogue.shared
        val notUnderTopic = cat.services.map { it.id }.filter { it !in r.viaTopics && it !in r.viaShelves }
        val topicSum = cat.topics.sumOf { it.services }
        return listOf(
            "catalogue: ${r.total} services (${cat.services.count { it.type == ServiceType.Standalone }} standalone, ${cat.bundles.size} bundles, ${cat.journeys.size} journeys)",
            "reachable: ${r.reachable.size} of ${r.total} · unreachable: ${r.unreachable.ifEmpty { listOf("none") }.joinToString(", ")}",
            "via topics: ${r.viaTopics.size} (topic counts add up to $topicSum) · via shelves: ${r.viaShelves.size} · via A to Z: ${r.viaAToZ.size} · via entities: ${r.viaEntities.size} (${ServicesData.owners.size} rows)",
            "not under a topic (reached from shelves, A to Z or entities instead): ${notUnderTopic.ifEmpty { listOf("none") }.joinToString(", ")}",
        ).joinToString("\n") { "[ServicesCheck] $it" }
    }

    private var hasRun = false

    /** Logs the summary once when the app is launched with `servicesCheck true`. */
    fun runIfRequested(context: Context) {
        if (hasRun || !ServicesDebug.flag("servicesCheck")) return
        hasRun = true
        val text = summary()
        text.lines().forEach { Log.i("ServicesCheck", it) }
        runCatching { File(context.filesDir, "services-check.txt").writeText(text + "\n") }
    }
}
