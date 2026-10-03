package com.salehtl.dubainownext.features.activity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.app.LocalRouter
import com.salehtl.dubainownext.app.Route
import com.salehtl.dubainownext.app.Router
import com.salehtl.dubainownext.app.SheetRoute
import com.salehtl.dubainownext.data.DraftItem
import com.salehtl.dubainownext.data.RequestItem
import com.salehtl.dubainownext.design.DNRadius
import com.salehtl.dubainownext.design.components.ComingSoon
import com.salehtl.dubainownext.design.components.DNListGroup
import com.salehtl.dubainownext.design.components.DNRowDivider

// Blocks shared by the Activity tab and the pushed Drafts and History screens.

/** Picking up a draft opens the service itself, which comes in the next phase of the demo. */
internal fun Router.activityOpenDraft(draft: DraftItem) {
    present(
        SheetRoute.ComingSoonSheet(
            ComingSoon(
                title = draft.title,
                detail = "You've done ${draft.stepsDone} of ${draft.stepsTotal} steps. Picking up a draft opens the service itself, which comes in the next phase of the demo.",
                owner = draft.activityOwner,
                symbol = draft.symbol,
                hue = draft.hue,
                kind = ComingSoon.Kind.ServicesPhase,
            ),
        ),
    )
}

/** In progress with government: one list group of rows (→ tracker). */
@Composable
internal fun ActivityInProgressGroup(requests: List<RequestItem>, modifier: Modifier = Modifier) {
    val router = LocalRouter.current
    DNListGroup(modifier) {
        requests.forEachIndexed { i, r ->
            ActivityRequestRow(r) { router.push(Route.Request(r.id)) }
            if (i < requests.lastIndex) DNRowDivider(inset = 68.dp)
        }
    }
}

/** Drafts in the All list: compact rows in one group, each one swipeable. */
@Composable
internal fun ActivityDraftRowsGroup(drafts: List<DraftItem>, onDelete: (DraftItem) -> Unit, modifier: Modifier = Modifier) {
    val router = LocalRouter.current
    DNListGroup(modifier) {
        drafts.forEachIndexed { i, d ->
            ActivityDraftSwipe(d, RectangleShape, onContinue = { router.activityOpenDraft(d) }, onDelete = { onDelete(d) }) { longClick, actions ->
                ActivityDraftRow(d, ActivityDraftPins.isPinned(d.id), onClick = { router.activityOpenDraft(d) }, onLongClick = longClick, actions = actions)
            }
            if (i < drafts.lastIndex) DNRowDivider(inset = 68.dp)
        }
    }
}

/** Drafts filter and the Drafts screen: one expanded card, swipeable. */
@Composable
internal fun ActivityDraftCardItem(draft: DraftItem, onDelete: (DraftItem) -> Unit, modifier: Modifier = Modifier) {
    val router = LocalRouter.current
    Column(modifier.fillMaxWidth()) {
        ActivityDraftSwipe(draft, RoundedCornerShape(DNRadius.brief), onContinue = { router.activityOpenDraft(draft) }, onDelete = { onDelete(draft) }) { longClick, actions ->
            ActivityDraftCard(
                draft, ActivityDraftPins.isPinned(draft.id),
                onContinue = { router.activityOpenDraft(draft) },
                onStartOver = { router.activityOpenDraft(draft) },
                onLongClick = longClick,
                actions = actions,
            )
        }
    }
}

/** One month of History: the receipts in a list group (→ receipt). */
@Composable
internal fun ActivityHistoryMonthGroup(month: ActivityFormat.Month, modifier: Modifier = Modifier) {
    val router = LocalRouter.current
    DNListGroup(modifier) {
        month.receipts.forEachIndexed { i, r ->
            ActivityHistoryRow(r) { router.push(Route.Receipt(r.id)) }
            if (i < month.receipts.lastIndex) DNRowDivider(inset = 68.dp)
        }
    }
}

internal const val ACTIVITY_DRAFTS_NOTICE =
    "Drafts are saved on every step and kept for 30 days after you last change them. We remind you 3 days before one is deleted."

internal const val ACTIVITY_DRAFTS_FOOTER =
    "Swipe a draft left to delete it, or right to pin it. Touch and hold for the same actions. Deleting asks to confirm, then you can undo for 5 seconds."

