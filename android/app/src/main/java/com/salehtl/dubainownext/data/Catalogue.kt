package com.salehtl.dubainownext.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The refactored DubaiNow service catalogue (assets/dubainow-services.json, 2 Oct 2026):
 * 219 services in 12 topics — 210 standalone, 4 bundles, 5 journeys — plus things and guides.
 * Call [Catalogue.load] once at start-up, then use [Catalogue.shared].
 */
@Serializable
data class Catalogue(
    val name: String,
    val version: String,
    val date: String,
    val topics: List<CatalogueTopic>,
    val services: List<CatalogueService>,
    val things: List<CatalogueService> = emptyList(),
    val guides: List<CatalogueService> = emptyList(),
) {
    companion object {
        lateinit var shared: Catalogue
            private set
        private lateinit var byId: Map<String, CatalogueService>

        fun load(context: Context) {
            if (::shared.isInitialized) return
            val text = context.assets.open("dubainow-services.json").bufferedReader().use { it.readText() }
            shared = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true; explicitNulls = false }.decodeFromString(serializer(), text)
            byId = (shared.services + shared.things + shared.guides).associateBy { it.id }
        }

        fun service(id: String): CatalogueService? = byId[id]
    }

    fun service(id: String): CatalogueService? = Companion.service(id)
    fun topic(id: String): CatalogueTopic? = topics.firstOrNull { it.id == id }

    /** Standalone services in a topic (and optionally a sub-topic), most used first. */
    fun standalone(inTopic: String, subtopic: String? = null): List<CatalogueService> =
        services.filter { it.type == ServiceType.Standalone && it.topic == inTopic && (subtopic == null || it.subtopic == subtopic) }
            .sortedByDescending { it.uniques30d }

    /** Every service in a topic including bundles and journeys that belong to it. */
    fun all(inTopic: String): List<CatalogueService> = services.filter { it.topic == inTopic }.sortedByDescending { it.uniques30d }

    val bundles: List<CatalogueService> get() = services.filter { it.type == ServiceType.Bundle }.sortedByDescending { it.uniques30d }
    val journeys: List<CatalogueService> get() = services.filter { it.type == ServiceType.Journey }

    /** All 219 services A to Z (standalone, bundles and journeys). */
    val aToZ: List<CatalogueService> get() = services.sortedBy { it.name.lowercase() }

    /** Owners shown as entities ("By government entity"), with their service counts. */
    val owners: List<Pair<String, Int>>
        get() = services.filter { it.owner != "DubaiNow" }
            .groupingBy { it.owner ?: "To confirm" }.eachCount()
            .toList().sortedBy { it.first.lowercase() }

    fun services(ownedBy: String): List<CatalogueService> =
        services.filter { (it.owner ?: "To confirm") == ownedBy }.sortedBy { it.name }

    /** Simple ranked search over names, aliases, owners and descriptions. Features can do better. */
    fun search(query: String): List<CatalogueService> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        fun score(s: CatalogueService): Int {
            val name = s.name.lowercase()
            return when {
                name.startsWith(q) -> 100
                name.split(" ").any { it.startsWith(q) } -> 80
                s.aliases.any { it.lowercase().startsWith(q) } -> 70
                name.contains(q) -> 60
                s.aliases.any { it.lowercase().contains(q) } -> 50
                (s.owner ?: "").lowercase().startsWith(q) -> 40
                s.description.lowercase().contains(q) -> 20
                else -> 0
            }
        }
        return (services + things).map { it to score(it) }.filter { it.second > 0 }
            .sortedWith(compareByDescending<Pair<CatalogueService, Int>> { it.second }.thenByDescending { it.first.uniques30d })
            .map { it.first }
    }
}

@Serializable
data class CatalogueTopic(
    val id: String,
    val name: String,
    val nameAr: String = "",
    /** Count of services in this topic (from the catalogue build). */
    val services: Int = 0,
    val things: List<String> = emptyList(),
    val journeys: List<String> = emptyList(),
    val bundles: List<String> = emptyList(),
    val owners: List<String> = emptyList(),
    val subtopics: List<CatalogueSubtopic> = emptyList(),
)

@Serializable
data class CatalogueSubtopic(val id: String, val name: String, val services: Int = 0)

@Serializable
enum class ServiceType {
    @kotlinx.serialization.SerialName("standalone") Standalone,
    @kotlinx.serialization.SerialName("bundle") Bundle,
    @kotlinx.serialization.SerialName("journey") Journey,
}

/** A service record (also used for things and guides, which share the shape). */
@Serializable
data class CatalogueService(
    val id: String,
    val kind: String = "service",          // service | thing | info
    val type: ServiceType = ServiceType.Standalone,
    val name: String,
    val nameAr: String = "",
    val shortName: String = "",
    val description: String = "",
    val topic: String? = null,
    val subtopic: String? = null,
    val owner: String? = null,
    val ownerConfirmed: Boolean = true,
    val audience: String = "all",
    val aliases: List<String> = emptyList(),
    val status: String = "live",           // live | beta | to publish | to confirm | proposed | maintenance
    val uniques30d: Int = 0,
    val parts: List<String>? = null,
    val entities: List<String>? = null,
    val stages: List<Stage>? = null,
    val lasts: String? = null,
    val scaleLine: String? = null,
    val countLine: String? = null,
    val covers: String? = null,
    val doesTheWork: String? = null,
    val views: List<String>? = null,
    val options: List<Option>? = null,
) {
    @Serializable
    data class Stage(val name: String, val takes: String? = null, val parts: List<String> = emptyList(), val notInDubaiNowYet: List<Missing> = emptyList())

    @Serializable
    data class Missing(val name: String, val owner: String? = null)

    @Serializable
    data class Option(val id: String, val name: String, val description: String? = null, val owner: String? = null)

    val short: String get() = shortName.ifEmpty { name }

    /** The owner as shown beside the name ("RTA"). Bundles and journeys show their entities. */
    val ownerLabel: String
        get() = when (type) {
            ServiceType.Standalone -> owner ?: "Owner to confirm"
            else -> {
                val e = entities ?: emptyList()
                if (e.size > 2) "${e.take(2).joinToString(", ")} and ${e.size - 2} more" else e.joinToString(" and ")
            }
        }

    val isLive: Boolean get() = status == "live" || status == "beta"

    /** Plain-English status for records that aren't simply live. */
    val statusLabel: String?
        get() = when (status) {
            "beta" -> "Beta"
            "maintenance" -> "Under maintenance"
            "to publish", "proposed", "to confirm" -> "Coming soon"
            else -> null
        }

    val audienceLabel: String?
        get() = when (audience) {
            "dubai-citizens" -> "For Dubai citizens"
            "nationals" -> "For UAE nationals"
            "property-owners" -> "For property owners"
            "vehicle-owners" -> "For vehicle owners"
            "parents" -> "For parents"
            "drivers" -> "For drivers"
            "pod" -> "For people of determination"
            "residents" -> "For residents"
            "tenants" -> "For tenants"
            "seniors" -> "For senior citizens"
            "healthcare-pros" -> "For healthcare professionals"
            "pensioners" -> "For pensioners"
            "businesses" -> "For businesses"
            "pet-owners" -> "For pet owners"
            "learners" -> "For learner drivers"
            "visitors" -> "For visitors"
            "guardians" -> "For guardians"
            "workers" -> "For workers"
            "students" -> "For students"
            "donors" -> "For donors"
            else -> null
        }

    /** Resolved parts (some referenced parts aren't records yet and are skipped). */
    val resolvedParts: List<CatalogueService> get() = (parts ?: emptyList()).mapNotNull { Catalogue.service(it) }

    /** "{n} bills and fines, one payment" with n filled in. */
    fun countLine(n: Int): String? = countLine?.replace("{n}", n.toString())
}
