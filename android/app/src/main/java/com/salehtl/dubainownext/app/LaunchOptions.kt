package com.salehtl.dubainownext.app

import android.content.Intent
import com.salehtl.dubainownext.data.DemoStore
import com.salehtl.dubainownext.data.PaymentPurpose
import com.salehtl.dubainownext.data.PaymentRequest
import com.salehtl.dubainownext.data.personas.Personas

/** Launch extras for demos and screenshots. Feature code may read its own extras via [LaunchOptions.extras]. */
data class LaunchOptions(
    val persona: String? = null,
    val tab: AppTab? = null,
    val routes: List<Route> = emptyList(),
    val sheet: String? = null,
    val assistant: Boolean = false,
    val hour: Int? = null,
    val welcome: Boolean = false,
) {
    companion object {
        /** All raw extras as strings (for feature-specific debug flags). */
        var extras: Map<String, String> = emptyMap()
            private set

        fun from(intent: Intent?): LaunchOptions {
            val b = intent?.extras ?: return LaunchOptions()
            extras = b.keySet().associateWith { b.getString(it) ?: "" }
            return LaunchOptions(
                persona = b.getString("persona"),
                tab = b.getString("tab")?.let { t -> AppTab.entries.firstOrNull { it.name.equals(t, ignoreCase = true) } },
                routes = b.getString("route")?.split(",")?.mapNotNull { Route.parse(it.trim()) } ?: emptyList(),
                sheet = b.getString("sheet"),
                assistant = b.getString("assistant") == "true",
                hour = b.getString("hour")?.toIntOrNull(),
                welcome = b.getString("welcome") == "true",
            )
        }

        fun flag(name: String) = extras[name]?.let { it == "true" || it.isEmpty() } ?: false
    }

    fun apply(store: DemoStore, router: Router) {
        store.hourOverride = hour
        if (!welcome && persona != null) {
            if (persona == "guest") store.continueAsGuest() else Personas.persona(persona)?.let { store.signIn(it) }
        }
        tab?.let { router.select(it) }
        routes.forEach { router.push(it) }
        when (sheet) {
            null -> Unit
            "payAll" -> router.present(SheetRoute.Payment(store.payAllRequest()))
            else -> SheetRoute.parse(sheet)?.let { router.present(it) }
        }
        if (assistant) router.isAssistantPresented = true
    }
}

@Suppress("unused")
private val unusedPurpose: PaymentPurpose? = null
@Suppress("unused")
private val unusedRequest: PaymentRequest? = null
