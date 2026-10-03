package com.salehtl.dubainownext.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetDefaults
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNRadius
import com.salehtl.dubainownext.design.components.ComingSoonContent
import com.salehtl.dubainownext.design.components.ToastHost
import com.salehtl.dubainownext.features.activity.DraftsScreen
import com.salehtl.dubainownext.features.activity.HistoryScreen
import com.salehtl.dubainownext.features.activity.PaymentSheet
import com.salehtl.dubainownext.features.activity.ReceiptScreen
import com.salehtl.dubainownext.features.activity.ReceiptSheet
import com.salehtl.dubainownext.features.activity.RequestTrackerScreen
import com.salehtl.dubainownext.features.home.EditFavoritesSheet
import com.salehtl.dubainownext.features.profile.DocumentScreen
import com.salehtl.dubainownext.features.profile.DocumentsScreen
import com.salehtl.dubainownext.features.profile.FamilyScreen
import com.salehtl.dubainownext.features.profile.HappinessMeterSheet
import com.salehtl.dubainownext.features.profile.NotificationsScreen
import com.salehtl.dubainownext.features.profile.ProfileSheet
import com.salehtl.dubainownext.features.profile.VehicleScreen
import com.salehtl.dubainownext.features.profile.VehiclesScreen
import com.salehtl.dubainownext.features.services.AllServicesScreen
import com.salehtl.dubainownext.features.services.BundleScreen
import com.salehtl.dubainownext.features.services.BundlesListScreen
import com.salehtl.dubainownext.features.services.EntitiesScreen
import com.salehtl.dubainownext.features.services.EntityScreen
import com.salehtl.dubainownext.features.services.JourneyScreen
import com.salehtl.dubainownext.features.services.JourneysListScreen
import com.salehtl.dubainownext.features.services.ServiceStartScreen
import com.salehtl.dubainownext.features.services.SubtopicScreen
import com.salehtl.dubainownext.features.services.TopicScreen
import com.salehtl.dubainownext.features.widgets.ExtendParkingSheet
import com.salehtl.dubainownext.features.widgets.TopUpSalikSheet
import com.salehtl.dubainownext.features.widgets.WidgetGallerySheet
import kotlinx.coroutines.launch

/** Maps every [Route] to its feature screen. These function names are the contract between features. */
@Composable
fun RouteDestination(route: Route) {
    when (route) {
        is Route.Topic -> TopicScreen(route.id)
        is Route.Subtopic -> SubtopicScreen(route.topic, route.sub)
        is Route.Service -> ServiceStartScreen(route.id)
        is Route.Bundle -> BundleScreen(route.id)
        is Route.Journey -> JourneyScreen(route.id)
        Route.AllServices -> AllServicesScreen()
        Route.Entities -> EntitiesScreen()
        is Route.Entity -> EntityScreen(route.name)
        Route.BundlesList -> BundlesListScreen()
        Route.JourneysList -> JourneysListScreen()
        is Route.Vehicle -> VehicleScreen(route.id)
        Route.Vehicles -> VehiclesScreen()
        Route.Documents -> DocumentsScreen()
        is Route.Document -> DocumentScreen(route.id)
        Route.Family -> FamilyScreen()
        is Route.Request -> RequestTrackerScreen(route.id)
        is Route.Receipt -> ReceiptScreen(route.id)
        Route.Drafts -> DraftsScreen()
        Route.History -> HistoryScreen()
        Route.Notifications -> NotificationsScreen(inSheet = false)
    }
}

/** Sheets that push pages inside themselves: system back goes up a page before it closes the sheet. */
private val SheetRoute.hasPages: Boolean get() = this is SheetRoute.Profile

/** True inside any sheet presented by [SheetHost] (sheets have no logo; close instead of back). */
val LocalInSheet = compositionLocalOf { false }

/** Presents the current [SheetRoute] as an M3 modal bottom sheet (radius 28, drag handle). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetHost() {
    val router = LocalRouter.current; val toasts = LocalToasts.current
    val sheet = router.sheet ?: return
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = { router.dismissSheet() },
        // On API 33+ the sheet's own back callback beats every BackHandler inside it, so a sheet
        // with pages (Profile) turns it off and handles back below. Others keep predictive back.
        properties = if (sheet.hasPages) ModalBottomSheetProperties(shouldDismissOnBackPress = false) else ModalBottomSheetDefaults.properties,
        // Tall sheets stop below the status bar (with a little of the app showing), not under it.
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(top = 8.dp),
        sheetState = state,
        containerColor = if (sheet is SheetRoute.ComingSoonSheet) DNColor.surfacePrimary else DNColor.surfaceSecondary,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = DNRadius.sheet, topEnd = DNRadius.sheet),
    ) {
        // Back closes the sheet. A sheet's own pages register their BackHandler later, so they pop first.
        BackHandler { scope.launch { state.hide() }.invokeOnCompletion { if (!state.isVisible) router.dismissSheet() } }
        CompositionLocalProvider(LocalInSheet provides true) {
            Box(Modifier.fillMaxWidth().navigationBarsPadding()) {
                when (sheet) {
                    SheetRoute.Profile -> ProfileSheet()
                    is SheetRoute.HappinessMeter -> HappinessMeterSheet(sheet.context)
                    SheetRoute.Notifications -> NotificationsScreen(inSheet = true)
                    is SheetRoute.Payment -> PaymentSheet(sheet.request)
                    is SheetRoute.Receipt -> ReceiptSheet(sheet.id)
                    SheetRoute.WidgetGallery -> WidgetGallerySheet()
                    SheetRoute.EditFavorites -> EditFavoritesSheet()
                    is SheetRoute.ComingSoonSheet -> Box(Modifier.verticalScroll(rememberScrollState())) {
                        ComingSoonContent(sheet.item) { router.dismissSheet() }
                    }
                    SheetRoute.ExtendParking -> ExtendParkingSheet()
                    SheetRoute.TopUpSalik -> TopUpSalikSheet()
                    SheetRoute.DemoMenu -> DemoMenuSheet()
                }
                if (sheet !is SheetRoute.Profile) ToastHost(toasts, bottomInset = 16.dp)
            }
        }
    }
}
