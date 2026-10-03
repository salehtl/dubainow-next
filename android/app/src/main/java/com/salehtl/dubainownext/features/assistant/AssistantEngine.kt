package com.salehtl.dubainownext.features.assistant

import android.view.View
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.salehtl.dubainownext.app.AppTab
import com.salehtl.dubainownext.app.LaunchOptions
import com.salehtl.dubainownext.app.Route
import com.salehtl.dubainownext.app.Router
import com.salehtl.dubainownext.app.SheetRoute
import com.salehtl.dubainownext.data.Catalogue
import com.salehtl.dubainownext.data.DemoDate
import com.salehtl.dubainownext.data.DemoStore
import com.salehtl.dubainownext.data.Money
import com.salehtl.dubainownext.data.NeedsYouItem
import com.salehtl.dubainownext.data.Payable
import com.salehtl.dubainownext.data.PaymentLine
import com.salehtl.dubainownext.data.PaymentMethod
import com.salehtl.dubainownext.data.PaymentPurpose
import com.salehtl.dubainownext.data.PaymentRequest
import com.salehtl.dubainownext.data.RequestItem
import com.salehtl.dubainownext.data.RequestStep
import com.salehtl.dubainownext.data.Vehicle
import com.salehtl.dubainownext.data.hue
import com.salehtl.dubainownext.data.symbol
import com.salehtl.dubainownext.design.DNHaptics
import com.salehtl.dubainownext.design.Hue
import com.salehtl.dubainownext.design.components.ComingSoon
import com.salehtl.dubainownext.design.components.HappinessFace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID

/**
 * The scripted assistant (demo only: no network, no AI). Port of iOS AssistantEngine.swift.
 * Everything it does goes through [DemoStore], so the screen underneath updates in place.
 * State is Compose state; scripts run as coroutines in the overlay's scope (cancelled on close).
 */
@Stable
internal class AssistantEngine(
    private val store: DemoStore,
    private val router: Router,
    private val scope: CoroutineScope,
) {
    var phase by mutableStateOf(AssistantPhase.Idle); private set
    var face by mutableStateOf(AssistantFaceState.Awake); private set
    /** Working: how far round the ring the light has filled (0…1). */
    var progress by mutableFloatStateOf(0f); private set
    var card by mutableStateOf<AssistantCard?>(null); private set
    var confirm by mutableStateOf<AssistantConfirm?>(null); private set
    var isConfirming by mutableStateOf(false); private set
    var draft by mutableStateOf("")
    /** Listening: words heard so far, and the word still being recognised (grey). */
    var heard by mutableStateOf(""); private set
    var hearing by mutableStateOf(""); private set
    /** Mandarin answers: show the English version instead. */
    var showsEnglish by mutableStateOf(false)
    private var methodIndex by mutableIntStateOf(0)
    /** Done-once intents (not suggested again in this session). */
    private var completed by mutableStateOf(setOf<String>())
    /** After a task, the single suggestion prefers this part of life. */
    private var lastTopic by mutableStateOf<String?>(null)
    /** Offer "Continue in English" after a Mandarin task. */
    private var offersEnglish by mutableStateOf(false)
    private var note: AssistantNote? = null

    // Wired by the overlay.
    var view: View? = null
    /** Fingerprint (BiometricPrompt) or a short confirm when the device has none. */
    var authenticate: suspend (title: String, reason: String) -> Boolean = { _, _ -> delay(900); true }
    /** Shows the note after a finished task closes (snackbar with "Receipt"). */
    var showNote: (AssistantNote) -> Unit = {}
    /** Reads a line out with TalkBack (headlines, "Your turn"), like iOS's announcements. */
    var announce: (String) -> Unit = {}
    /** "Start scanning": opens the tracker's fake passport camera for this request. */
    var scanPassport: (requestID: String) -> Unit = {}

    private var job: Job? = null
    private var onConfirm: (suspend (PaymentMethod) -> Unit)? = null
    private var takeOverTarget: AssistantFollowUp? = null
    private var listeningSentence = ""

    // MARK: Ways in

    /** Called when the overlay appears: hold-to-talk, a seeded question, or the debug extras. */
    fun start(seed: String?) {
        val launch = AssistantLaunch.take()
        if (launch.payFinesFirst) payRenewalFinesFirst()
        when {
            seed == LISTEN_SEED || launch.listen -> listen()
            !seed.isNullOrEmpty() -> run(seed)
            launch.run != null -> run(launch.run)
        }
    }

    /** Debug (`assistantPaidFine true`): the fines blocking the renewal are already paid (AED 420 path). */
    private fun payRenewalFinesFirst() {
        val fines = store.assistantRenewalPlan()?.fines ?: return
        if (fines.isEmpty()) return
        store.completePayment(PaymentRequest(
            id = "debug-fines", title = "Pay ${fines.size} fines",
            lines = fines.map { PaymentLine(it.title, it.entity, it.amount, it.symbol, it.hue) },
            purpose = PaymentPurpose.Payables(fines.map { it.id }),
        ))
    }

    /** Hold to talk (or the mic): a live fake transcript types itself out, then sends. */
    fun listen(sentence: String? = null) {
        cancelJob()
        val text = sentence ?: listenSentence()
        listeningSentence = text
        card = null
        confirm = null
        heard = ""
        hearing = ""
        set(AssistantPhase.Listening, AssistantFaceState.Listening)
        val mandarin = AssistantIntent.isMandarin(text)
        val tokens = tokens(text, mandarin)
        job = scope.launch {
            delay(450)
            tokens.forEachIndexed { i, token ->
                hearing = token
                delay(if (mandarin) 150 else 240)
                heard += token
                hearing = if (i + 1 < tokens.size) tokens[i + 1] else ""
            }
            delay(650)
            run(text)
        }
    }

    /** Send what's been heard straight away. */
    fun sendListening() = run(listeningSentence)

    fun sendDraft() {
        val text = draft
        draft = ""
        run(text)
    }

    /** Runs a question or a task. */
    fun run(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val intent = AssistantIntent.match(trimmed)
        if (intent == AssistantIntent.ContinueInEnglish && card?.isMandarin == true) {
            showsEnglish = true
            offersEnglish = false
            announce(card?.headline?.en ?: "")
            return
        }
        cancelJob()
        draft = ""
        confirm = null
        onConfirm = null
        takeOverTarget = null
        note = null
        offersEnglish = false
        progress = 0f
        showsEnglish = when (intent) {
            is AssistantIntent.EmployerComplaint -> !intent.mandarin
            is AssistantIntent.Unknown -> !intent.mandarin
            else -> true
        }
        if (AssistantLaunch.forceEnglish) showsEnglish = true
        card = AssistantCard(
            quote = AssistantText(trimmed),
            headline = AssistantText("Thinking", zh = if (AssistantIntent.isMandarin(trimmed)) "思考中" else null),
            isThinking = true,
        )
        set(AssistantPhase.Thinking, AssistantFaceState.Thinking)
        job = scope.launch {
            delay(1100)
            perform(intent, trimmed)
        }
    }

    // MARK: Ways out

    /** Close (×), swipe the ask bar down, tap the screen, or system back. */
    fun close() {
        // Already closing (the overlay fades out): don't close twice or show the note again.
        if (!router.isAssistantPresented) return
        cancelJob()
        val n = note
        router.closeAssistant()
        if (n != null && (face == AssistantFaceState.Done || face == AssistantFaceState.Quiet)) showNote(n)
    }

    /** Stop sits on every task card. Nothing is paid or sent. */
    fun stop() {
        cancelJob()
        onConfirm = null
        confirm = null
        isConfirming = false
        var c = card ?: return
        c = when (val s = c.surface) {
            is AssistantSurface.Task -> {
                val t = s.block
                c.copy(surface = AssistantSurface.Task(t.copy(
                    status = AssistantTaskStatus.Stopped,
                    steps = t.steps.map { if (it.state != AssistantStepState.Done) it.copy(state = AssistantStepState.Pending) else it },
                )))
            }
            is AssistantSurface.Complaint -> c.copy(surface = AssistantSurface.Complaint(s.block.copy(status = AssistantTaskStatus.Stopped)))
            else -> c
        }
        card = c.copy(
            headline = AssistantText("Stopped. Nothing was paid or sent.", zh = if (c.isMandarin) "已停止。没有付款，也没有提交任何内容。" else null),
            controls = AssistantControls.None,
        )
        progress = 0f
        set(AssistantPhase.Card, AssistantFaceState.Awake)
        announce(card?.headline?.resolved(showsEnglish) ?: "")
    }

    /** "I'll do it myself": opens the service at the same step. */
    fun takeOver() {
        val target = takeOverTarget
        cancelJob()
        hand(target)
    }

    fun perform(followUp: AssistantFollowUp) {
        cancelJob()
        // The camera opens over the assistant, which closes once the photo is sent.
        if (followUp is AssistantFollowUp.ScanPassport) scanPassport(followUp.requestID) else hand(followUp)
    }

    private fun hand(target: AssistantFollowUp?) {
        router.closeAssistant()
        when (target) {
            null, is AssistantFollowUp.ScanPassport -> Unit
            is AssistantFollowUp.Push -> if (router.stack(router.tab).lastOrNull() != target.route) router.push(target.route)
            is AssistantFollowUp.Present -> router.present(target.sheet)
            is AssistantFollowUp.OpenService -> router.open(target.id)
        }
    }

    // MARK: Your turn

    val paymentMethod: PaymentMethod
        get() {
            val methods = store.state.paymentMethods
            if (methods.isEmpty()) return PaymentMethod.GooglePay
            return methods[methodIndex % methods.size]
        }

    val canChangeMethod: Boolean get() = store.state.paymentMethods.size > 1

    fun changeMethod() {
        view?.let { DNHaptics.selection(it) }
        methodIndex += 1
    }

    /** The one confirm per task: fingerprint (BiometricPrompt) or a short confirm without biometrics. */
    fun confirmTapped() {
        val action = onConfirm ?: return
        val c = confirm ?: return
        if (isConfirming) return
        isConfirming = true
        val method = paymentMethod
        job = scope.launch {
            val title = if (c.isUaePass) "Confirm with UAE PASS" else c.title.en
            val ok = authenticate(title, c.reason)
            isConfirming = false
            if (!ok) return@launch
            onConfirm = null
            confirm = null
            action(method)
        }
    }

    fun rate(face: HappinessFace, receiptID: String) = store.rate(receiptID, face)

    // MARK: Suggestions

    /** Up to three for the screen you're on, and one after a task. Never a button already on screen. */
    fun suggestions(): List<AssistantSuggestion> = when (phase) {
        AssistantPhase.Listening, AssistantPhase.Thinking -> emptyList()
        AssistantPhase.Idle -> available().take(3)
        AssistantPhase.Card -> cardSuggestions()
    }

    private fun cardSuggestions(): List<AssistantSuggestion> {
        if (confirm != null) return emptyList()
        if (offersEnglish && !showsEnglish) return listOf(AssistantSuggestion("Continue in English", "character.bubble", Hue.Sky))
        val s = card?.surface
        if (s is AssistantSurface.Task && s.block.status == AssistantTaskStatus.Working) return emptyList()
        if (s is AssistantSurface.Complaint && s.block.status == AssistantTaskStatus.Working) return emptyList()
        // Never suggest what you just asked ("Scan my passport with me" twice).
        val asked = setOfNotNull(card?.quote?.en, card?.quote?.zh)
        val all = available().filter { it.text !in asked }
        if (card?.rating != null || face == AssistantFaceState.Done || face == AssistantFaceState.Quiet) {
            val same = all.firstOrNull { AssistantIntent.match(it.text).topic == lastTopic }
            return listOfNotNull(same ?: all.firstOrNull())
        }
        if (card?.headline?.en?.startsWith("I can't") == true) return all.take(3)
        return all.take(1)
    }

    private fun available(): List<AssistantSuggestion> {
        val texts = mutableListOf<String>()
        // On a request tracker that's waiting for you: help with that step (Paper UR2-0).
        val last = router.stack(router.tab).lastOrNull()
        if (last is Route.Request) {
            val r = store.request(last.id)
            if (r != null && r.status is RequestItem.Status.YourTurn) {
                val step = r.currentStepIndex?.let { r.steps[it].title.lowercase() } ?: ""
                if (step.contains("passport")) texts += "Scan my passport with me"
                if (r.steps.any { it.title.lowercase().contains("fee") && it.state == RequestStep.State.Upcoming }) texts += "What will the fees be?"
                texts += "Remind me tonight at 8"
                return texts.map { suggestion(it) }
            }
        }
        val persona = store.persona
        val prompts = (persona?.assistantPrompts ?: emptyList()).toMutableList()
        if (persona?.id == "wei") {
            // Wei's showcase is Mandarin; independent of his data.
            if (prompts.none { AssistantIntent.isMandarin(it) }) prompts.add(0, WEI_PROMPT)
            if (prompts.none { it.lowercase().contains("nol") }) prompts += "Top up my nol card with AED 50"
        }
        val onHomeRoot = router.tab == AppTab.Home && router.stack(AppTab.Home).isEmpty()
        for (p in prompts) {
            val intent = AssistantIntent.match(p)
            if (intent.key in completed) continue
            // Never repeat a button that's already on the screen (Pay all on Home).
            if (intent is AssistantIntent.Owe && onHomeRoot && store.showsPayAll) continue
            if (intent == AssistantIntent.RenewRegistration && store.assistantRenewalPlan() == null) continue
            texts += p
        }
        return texts.map { suggestion(it) }
    }

    /** What hold-to-talk "hears": the first suggestion for this screen. */
    private fun listenSentence(): String {
        if (store.persona?.id == "wei") return WEI_PROMPT
        available().firstOrNull()?.let { return it.text }
        return store.persona?.assistantPrompts?.firstOrNull() ?: "How much do I owe?"
    }

    // MARK: Scripts

    private suspend fun perform(intent: AssistantIntent, quote: String) {
        when (intent) {
            AssistantIntent.RenewRegistration -> runRenewal(quote)
            is AssistantIntent.Owe -> runOwe(quote, intent.business)
            AssistantIntent.NeedsYou -> runNeedsYou(quote)
            AssistantIntent.VisaStatus -> runVisa(quote)
            is AssistantIntent.SalikTopUp -> runTopUp(quote, salik = true, requested = intent.amount)
            is AssistantIntent.NolTopUp -> runTopUp(quote, salik = false, requested = intent.amount)
            is AssistantIntent.Journey -> runJourney(quote, intent.hint)
            AssistantIntent.VehicleFines -> runVehicleFines(quote)
            AssistantIntent.Energy -> runEnergy(quote)
            AssistantIntent.Metro -> runMetro(quote)
            AssistantIntent.Fees -> runFees(quote)
            AssistantIntent.Remind -> runRemind(quote)
            AssistantIntent.ScanPassport -> runScan(quote)
            is AssistantIntent.EmployerComplaint -> runComplaint(quote, intent.mandarin)
            AssistantIntent.ContinueInEnglish, is AssistantIntent.Unknown -> runFallback(quote, AssistantIntent.isMandarin(quote))
        }
    }

    // 1 · Renew my car registration (Paper TA7-0 → TQX-0 → U2V-0)
    private suspend fun runRenewal(quote: String) {
        val plan = store.assistantRenewalPlan()
        if (plan == null) {
            show(AssistantCard(
                quote = AssistantText(quote),
                headline = AssistantText("Your vehicles are all registered. Nothing needs renewing right now."),
                controls = AssistantControls.Action(AssistantText("See your vehicles"), AssistantFollowUp.Push(Route.Vehicles)),
            ), AssistantFaceState.Awake)
            return
        }
        val v = plan.vehicle
        val finesTotal = plan.finesTotal
        val steps = mutableListOf(AssistantStep(0, AssistantText("Insurance and test are valid")))
        if (plan.fines.isNotEmpty()) {
            val t = if (plan.fines.size == 1) "1 fine blocks the renewal" else "${plan.fines.size} fines block the renewal"
            steps += AssistantStep(1, AssistantText(t), finesTotal)
        }
        steps += AssistantStep(2, AssistantText("Preparing the renewal"), plan.fee)
        steps += AssistantStep(3, AssistantText("Your OK to pay"), plan.total)
        steps += AssistantStep(4, AssistantText("Pay, renew, save the new registration card"))

        val headline = when (plan.fines.size) {
            0 -> "Your ${v.model}'s registration ends on ${DemoDate.long(v.registrationExpiry)}. I'll renew it. You'll only need to confirm once."
            1 -> "A ${plan.fines[0].title.lowercase()} is blocking the renewal. I'll clear it first, then renew. You'll only need to confirm once."
            else -> "${plan.fines.size} fines are blocking the renewal. I'll clear them first, then renew. You'll only need to confirm once."
        }
        var task = AssistantTaskBlock(
            symbol = "car.fill", hue = Hue.Sky,
            title = AssistantText("Renew registration"),
            subtitle = AssistantText("${v.shortName} · ${v.plate.code} ${v.plate.number}"),
            steps = steps,
        )
        takeOverTarget = AssistantFollowUp.OpenService("renew-vehicle-registration")
        show(AssistantCard(AssistantText(quote), AssistantText(headline), surface = AssistantSurface.Task(task), controls = AssistantControls.Task), AssistantFaceState.Working)

        // Checks and preparation, one step at a time; the ring fills as steps finish.
        val n = steps.size.toFloat()
        for (i in 0 until steps.size - 2) {
            task = task.withStep(i, AssistantStepState.Active)
            update(task)
            delay(if (i == steps.size - 3) 1400 else 800)
            val ready = task.steps[i].title.en == "Preparing the renewal"
            task = task.withStep(i, AssistantStepState.Done, if (ready) AssistantText("Renewal is ready") else null)
            update(task)
            progress = (i + 1) / n
        }

        // Your turn: one confirm, under the thumb.
        task = task.withStep(steps.size - 2, AssistantStepState.YourTurn).copy(status = AssistantTaskStatus.YourTurn)
        val total = Money.aed(plan.total, trimWhole = true)
        val ready = if (plan.fines.isEmpty()) "Ready. The renewal comes to $total. Pay once and I'll do the rest."
        else "Ready. The fine and the renewal come to $total. Pay once and I'll do the rest."
        card?.let { card = it.copy(headline = AssistantText(ready), surface = AssistantSurface.Task(task)) }
        val lines = listOf(PaymentLine("Vehicle registration", "RTA · ${v.shortName}", plan.fee, "car.fill", Hue.Sky)) +
            plan.fines.map { PaymentLine(it.title, it.entity, it.amount, it.symbol, it.hue) }
        val request = PaymentRequest(
            id = "assistant-renewal-${v.id}",
            title = when (plan.fines.size) {
                0 -> "Renew registration"
                1 -> "Renew registration and pay a fine"
                else -> "Renew registration and pay ${plan.fines.size} fines"
            },
            lines = lines,
            purpose = PaymentPurpose.VehicleRenewal(vehicleID = v.id, fee = plan.fee, fineIDs = plan.fines.map { it.id }),
            doneByAssistant = true,
        )
        val readyTask = task
        yourTurn(AssistantConfirm(AssistantConfirm.Style.Pay(plan.total), AssistantText("Pay $total"), reason = "Pay $total to renew your registration")) { method ->
            finishRenewal(plan, request, readyTask, method)
        }
    }

    private suspend fun finishRenewal(plan: AssistantRenewalPlan, request: PaymentRequest, readyTask: AssistantTaskBlock, method: PaymentMethod) {
        val last = readyTask.steps.size - 1
        var task = readyTask.withStep(last - 1, AssistantStepState.Done).withStep(last, AssistantStepState.Active).copy(status = AssistantTaskStatus.Working)
        update(task)
        set(AssistantPhase.Card, AssistantFaceState.Working)
        progress = last.toFloat() / task.steps.size
        delay(900)

        val receipt = store.completePayment(request, method)
        view?.let { DNHaptics.success(it) }
        val v = store.state.vehicles.firstOrNull { it.id == plan.vehicle.id } ?: plan.vehicle
        val until = AssistantFormat.dayMonthYear(v.registrationExpiry)
        val rows = mutableListOf<AssistantStep>()
        if (plan.fines.isNotEmpty()) {
            rows += AssistantStep(0, AssistantText(if (plan.fines.size == 1) "Fine paid" else "Fines paid"), plan.finesTotal, AssistantStepState.Done)
        }
        rows += AssistantStep(1, AssistantText("Registration renewed"), plan.fee, AssistantStepState.Done)
        rows += AssistantStep(2, AssistantText("Valid until ${DemoDate.full(v.registrationExpiry)}"), state = AssistantStepState.Done)
        rows += AssistantStep(3, AssistantText("New card saved to Documents"), state = AssistantStepState.Done)
        task = task.copy(
            steps = rows, status = AssistantTaskStatus.Done, receiptID = receipt.id, reference = receipt.reference,
            paidLine = "Paid ${Money.aed(request.total)} · ${method.label}",
        )
        val headline = if (plan.fines.isEmpty()) "Done. Your ${v.model} is registered until $until."
        else "Done. Your ${v.model} is registered until $until, and the fine is paid."
        finish(
            headline = AssistantText(headline),
            surface = AssistantSurface.Task(task),
            rating = AssistantRatingPrompt("How was renewing your registration?", "One tap. It goes to RTA and the Happiness Meter.", receipt.id, "Thanks. RTA and the Happiness Meter will see this."),
            note = AssistantNote("${v.model} renewed", receipt.id),
            intent = AssistantIntent.RenewRegistration,
        )
    }

    // 2 · How much do I owe? / 6 · Pay everything for the café
    private fun runOwe(quote: String, business: Boolean) {
        val items = store.payables
        if (items.isEmpty()) {
            show(AssistantCard(AssistantText(quote), AssistantText("You don't owe anything right now. I'll tell you when a bill or fine comes in.")), AssistantFaceState.Awake)
            return
        }
        val total = store.payAllTotal
        val headline = when {
            business -> "Everything due for the café comes to ${Money.aed(total)}: ${if (items.size == 1) "1 payment" else "${items.size} bills and fines"}. Pay once and I'll do the rest."
            items.size == 1 -> "You owe ${Money.aed(total)}: your ${items[0].title.lowercase()} from ${items[0].entity}."
            else -> "You owe ${Money.aed(total)} across ${items.size} payments. Pay them together with one confirm."
        }
        val request = (if (items.size == 1) store.paymentRequest(items[0]) else store.payAllRequest()).copy(doneByAssistant = true)
        takeOverTarget = AssistantFollowUp.Present(SheetRoute.Payment(request))
        show(AssistantCard(AssistantText(quote), AssistantText(headline), surface = AssistantSurface.Payables(items), controls = AssistantControls.Task), AssistantFaceState.YourTurn)
        val title = if (items.size == 1) "Pay ${Money.aed(total)}" else "Pay all · ${Money.aed(total)}"
        yourTurn(AssistantConfirm(AssistantConfirm.Style.Pay(total), AssistantText(title), reason = "Pay ${Money.aed(total)}")) { method ->
            finishPayAll(items, request, method, AssistantIntent.Owe(business))
        }
    }

    private suspend fun finishPayAll(items: List<Payable>, request: PaymentRequest, method: PaymentMethod, intent: AssistantIntent) {
        var task = AssistantTaskBlock(
            symbol = "creditcard.fill", hue = Hue.Sky,
            title = AssistantText(if (items.size == 1) items[0].title else "Pay all"),
            subtitle = AssistantText(if (items.size == 1) items[0].entity else "${items.size} bills and fines"),
            steps = items.mapIndexed { i, p -> AssistantStep(i, AssistantText(p.title), p.amount, AssistantStepState.Active) },
        )
        card?.let { card = it.copy(surface = AssistantSurface.Task(task), controls = AssistantControls.None) }
        set(AssistantPhase.Card, AssistantFaceState.Working)
        progress = 0.6f
        delay(900)

        val receipt = store.completePayment(request, method)
        view?.let { DNHaptics.success(it) }
        task = task.copy(
            steps = task.steps.map { it.copy(state = AssistantStepState.Done) },
            status = AssistantTaskStatus.Done, receiptID = receipt.id, reference = receipt.reference,
            paidLine = "Paid ${Money.aed(request.total)} · ${method.label}",
        )
        val rest = if (store.payables.isEmpty()) " Nothing else is due." else ""
        finish(
            headline = AssistantText("Done. You've paid ${Money.aed(request.total)}.$rest"),
            surface = AssistantSurface.Task(task),
            rating = AssistantRatingPrompt("How was paying?", "One tap. It goes to the Happiness Meter.", receipt.id, "Thanks. The Happiness Meter will see this."),
            note = AssistantNote("Paid ${Money.aed(request.total, trimWhole = true)}", receipt.id),
            intent = intent,
        )
    }

    // What do I need to do?
    private fun runNeedsYou(quote: String) {
        val items = store.state.needsYou
        if (items.isEmpty()) {
            show(AssistantCard(AssistantText(quote), AssistantText("Nothing needs you right now. I'll tell you when something does.")), AssistantFaceState.Awake)
            return
        }
        val week = store.dueThisWeekCount
        val headline = (if (items.size == 1) "1 thing needs" else "${items.size} things need") + " you" + (if (week > 0) ", $week this week." else ".")
        val rows = items.take(4).map { item ->
            AssistantListRow(
                id = item.id, symbol = item.symbol, hue = item.hue, title = item.title,
                detail = item.due?.let { "${item.entity} · ${DemoDate.dueLabel(it)}" } ?: item.entity,
                value = item.amount?.let { Money.aed(it) },
            )
        }
        show(AssistantCard(AssistantText(quote), AssistantText(headline), surface = AssistantSurface.Rows(rows)), AssistantFaceState.Awake)
    }

    // 3 · When will my visa renewal be done?
    private fun runVisa(quote: String) {
        val visas = store.state.requests.filter { (it.title + it.serviceID).lowercase().contains("visa") }
        val r = visas.firstOrNull { it.status != RequestItem.Status.Done } ?: visas.firstOrNull()
        if (r == null) {
            val doc = store.state.documents.firstOrNull { it.title.lowercase().contains("visa") }
            val expiry = doc?.expiry
            if (doc != null && expiry != null) {
                val days = DemoDate.daysFromToday(expiry)
                val headline = if (days > 90) "Your ${doc.title.lowercase()} is valid until ${DemoDate.full(expiry)}. Nothing to do yet. I'll remind you 90 days before."
                else "Your ${doc.title.lowercase()} ends on ${DemoDate.full(expiry)}, in $days days. You can renew it now."
                show(AssistantCard(
                    AssistantText(quote), AssistantText(headline),
                    surface = AssistantSurface.Rows(listOf(AssistantListRow(doc.id, doc.symbol, doc.hue, doc.title, doc.number, DemoDate.short(expiry)))),
                    controls = AssistantControls.Action(AssistantText("See the document"), AssistantFollowUp.Push(Route.Document(doc.id))),
                ), AssistantFaceState.Awake)
                completed = completed + AssistantIntent.VisaStatus.key
                return
            }
            show(AssistantCard(
                AssistantText(quote), AssistantText("You don't have a visa renewal in progress."),
                controls = AssistantControls.Action(AssistantText("Renew a residence visa"), AssistantFollowUp.OpenService("renew-residence-visa")),
            ), AssistantFaceState.Awake)
            return
        }
        var action = AssistantText("Open the tracker")
        val headline = when (val s = r.status) {
            is RequestItem.Status.YourTurn -> {
                val ask = store.state.needsYou.firstOrNull { (it.kind as? NeedsYouItem.Kind.RequestStep)?.requestID == r.id }?.title
                    ?: r.currentStepIndex?.let { r.steps[it].title } ?: "Finish your step"
                action = AssistantText(r.action ?: "Open the tracker")
                "It's waiting for you: ${AssistantFormat.lowerFirst(ask)} by ${DemoDate.long(s.due)}. Then ${r.entity} usually decides within 2 working days."
            }
            is RequestItem.Status.InReview -> "${r.entity} is reviewing it. You should hear back by ${DemoDate.long(s.answerBy)}."
            is RequestItem.Status.Approved -> "It's approved: ${s.note}."
            RequestItem.Status.Done -> "It's done. Your new visa is in Documents."
        }
        show(AssistantCard(
            AssistantText(quote), AssistantText(headline), surface = AssistantSurface.Request(r),
            controls = AssistantControls.Action(action, AssistantFollowUp.Push(Route.Request(r.id))),
        ), AssistantFaceState.Awake)
        completed = completed + AssistantIntent.VisaStatus.key
    }

    // 4 · Top up Salik with AED 100 (and nol)
    private suspend fun runTopUp(quote: String, salik: Boolean, requested: BigDecimal) {
        val amount = requested.max(BigDecimal.ONE)
        val nol = store.state.nolBalance
        val balance: BigDecimal = when {
            salik -> store.state.salikBalance
            nol != null -> nol
            else -> {
                show(AssistantCard(
                    AssistantText(quote), AssistantText("I can't find a nol card on your account yet."),
                    controls = AssistantControls.Action(AssistantText("Top up a nol card"), AssistantFollowUp.OpenService("top-up-nol-card")),
                ), AssistantFaceState.Awake)
                return
            }
        }
        val name = if (salik) "Salik" else "nol card"
        val tags = store.state.vehicles.mapNotNull { it.salikTag }.size
        val headline = if (salik) {
            val trips = balance.divide(BigDecimal(4), 0, RoundingMode.DOWN).toInt()
            "Your Salik balance is ${Money.aed(balance, trimWhole = true)}, about $trips trips. I'll top it up with ${Money.aed(amount, trimWhole = true)}."
        } else "Your nol card has ${Money.aed(balance)}. I'll top it up with ${Money.aed(amount, trimWhole = true)}."
        var task = AssistantTaskBlock(
            symbol = if (salik) "road.lanes" else "tram.fill", hue = if (salik) Hue.Sand else Hue.Sky,
            title = AssistantText(if (salik) "Top up Salik" else "Top up your nol card"),
            subtitle = AssistantText(if (salik) "${if (tags == 1) "1 tag" else "$tags tags"} · balance ${Money.aed(balance)}" else "Balance ${Money.aed(balance)}"),
            steps = listOf(
                AssistantStep(0, AssistantText(if (salik) "Salik account found" else "nol card found")),
                AssistantStep(1, AssistantText("Your OK to pay"), amount),
                AssistantStep(2, AssistantText("Pay and update your balance")),
            ),
        )
        takeOverTarget = if (salik) AssistantFollowUp.Present(SheetRoute.TopUpSalik) else AssistantFollowUp.OpenService("top-up-nol-card")
        show(AssistantCard(AssistantText(quote), AssistantText(headline), surface = AssistantSurface.Task(task), controls = AssistantControls.Task), AssistantFaceState.Working)
        task = task.withStep(0, AssistantStepState.Active)
        update(task)
        delay(800)
        task = task.withStep(0, AssistantStepState.Done).withStep(1, AssistantStepState.YourTurn).copy(status = AssistantTaskStatus.YourTurn)
        update(task)
        progress = 1f / 3
        val request = PaymentRequest(
            id = "assistant-${if (salik) "salik" else "nol"}-${UUID.randomUUID().toString().take(6)}",
            title = if (salik) "Top up Salik" else "Top up nol card",
            lines = listOf(PaymentLine("$name top-up", if (salik) "Salik" else "RTA", amount, task.symbol, task.hue)),
            purpose = if (salik) PaymentPurpose.SalikTopUp(amount) else PaymentPurpose.NolTopUp(amount),
            doneByAssistant = true,
        )
        val label = Money.aed(amount, trimWhole = true)
        val readyTask = task
        yourTurn(AssistantConfirm(AssistantConfirm.Style.Pay(amount), AssistantText("Pay $label"), reason = "Pay $label to top up your $name")) { method ->
            finishTopUp(salik, request, readyTask, method)
        }
    }

    private suspend fun finishTopUp(salik: Boolean, request: PaymentRequest, readyTask: AssistantTaskBlock, method: PaymentMethod) {
        var task = readyTask.withStep(1, AssistantStepState.Done).withStep(2, AssistantStepState.Active).copy(status = AssistantTaskStatus.Working)
        update(task)
        set(AssistantPhase.Card, AssistantFaceState.Working)
        progress = 2f / 3
        delay(800)
        val receipt = store.completePayment(request, method)
        view?.let { DNHaptics.success(it) }
        val balance = if (salik) store.state.salikBalance else (store.state.nolBalance ?: BigDecimal.ZERO)
        task = task.copy(
            steps = listOf(
                AssistantStep(0, AssistantText("Topped up"), request.total, AssistantStepState.Done),
                AssistantStep(1, AssistantText("New balance"), balance, AssistantStepState.Done),
            ),
            status = AssistantTaskStatus.Done, receiptID = receipt.id, reference = receipt.reference,
            paidLine = "Paid ${Money.aed(request.total)} · ${method.label}",
        )
        val owner = if (salik) "Salik" else "RTA"
        finish(
            headline = AssistantText("Done. Your ${if (salik) "Salik balance" else "nol card balance"} is now ${Money.aed(balance)}."),
            surface = AssistantSurface.Task(task),
            rating = AssistantRatingPrompt(
                if (salik) "How was topping up Salik?" else "How was topping up your nol card?",
                "One tap. It goes to $owner and the Happiness Meter.", receipt.id, "Thanks. $owner and the Happiness Meter will see this.",
            ),
            note = AssistantNote(if (salik) "Salik topped up" else "nol card topped up", receipt.id),
            intent = if (salik) AssistantIntent.SalikTopUp(request.total) else AssistantIntent.NolTopUp(request.total),
        )
    }

    // 5 · What's next for my house?
    private fun runJourney(quote: String, hint: String) {
        val wanted: String? = when {
            listOf("house", "build", "villa", "drawing").any { hint.contains(it) } -> "build-a-house"
            hint.contains("move") -> "move-home"
            else -> null
        }
        val started = store.state.journeys
        val pick = started.firstOrNull { it.journeyID == wanted } ?: if (wanted == null) started.firstOrNull() else null
        val service = pick?.let { Catalogue.service(it.journeyID) }
        val stages = service?.stages
        if (pick == null || service == null || stages.isNullOrEmpty()) {
            val s = wanted?.let { Catalogue.service(it) }
            val sStages = s?.stages
            if (wanted != null && s != null && !sStages.isNullOrEmpty()) {
                show(AssistantCard(
                    AssistantText(quote),
                    AssistantText("You haven't started this yet. ${s.short} takes ${sStages.size} stages, from ${sStages.first().name.lowercase()} to ${sStages.last().name.lowercase()}. I can show you where to begin."),
                    controls = AssistantControls.Action(AssistantText("See the journey"), AssistantFollowUp.Push(Route.Journey(wanted))),
                ), AssistantFaceState.Awake)
            } else {
                show(AssistantCard(
                    AssistantText(quote),
                    AssistantText("You haven't started a journey yet. Journeys take you through something big, start to finish."),
                    controls = AssistantControls.Action(AssistantText("See all journeys"), AssistantFollowUp.Push(Route.JourneysList)),
                ), AssistantFaceState.Awake)
            }
            return
        }
        val i = pick.stageIndex.coerceIn(0, stages.size - 1)
        val waiting = pick.waitingOn.replace(" · ", ", ")
        val headline = if (pick.needsYou)
            "You're at stage ${i + 1} of ${stages.size}: ${AssistantFormat.lowerFirst(stages[i].name)}. It's your turn to ${AssistantFormat.lowerFirst(pick.headline)}. $waiting."
        else "You're at stage ${i + 1} of ${stages.size}: ${AssistantFormat.lowerFirst(stages[i].name)}. $waiting."
        val block = AssistantJourneyBlock(
            symbol = service.symbol, hue = service.hue, title = service.short,
            subtitle = listOfNotNull(startedLabel(pick.startedOn), pick.destination).joinToString(" · "),
            stage = i, stageCount = stages.size, now = pick.headline, nowDetail = pick.waitingOn,
            next = if (i + 1 < stages.size) stages[i + 1].name else null,
        )
        show(AssistantCard(
            AssistantText(quote), AssistantText(headline), surface = AssistantSurface.Journey(block),
            controls = AssistantControls.Action(AssistantText("Open the journey"), AssistantFollowUp.Push(Route.Journey(pick.journeyID))),
        ), AssistantFaceState.Awake)
        completed = completed + AssistantIntent.Journey("").key
    }

    /** "Started 4 days ago", "Started yesterday", "Started on 25 Jan" */
    private fun startedLabel(d: LocalDateTime): String {
        val n = DemoDate.daysFromToday(d)
        return if (n >= -6) "Started ${DemoDate.relative(d).lowercase()}" else "Started on ${DemoDate.short(d)}"
    }

    // Faisal · Which van has a fine?
    private fun runVehicleFines(quote: String) {
        val fined = store.state.vehicles.mapIndexedNotNull { i, v ->
            val fines = store.payables.filter { it.id in v.fineIDs }
            if (fines.isEmpty()) null else Triple(i, v, fines)
        }
        val first = fined.firstOrNull()
        if (first == null) {
            show(AssistantCard(AssistantText(quote), AssistantText("None of your vehicles has a fine right now.")), AssistantFaceState.Awake)
            return
        }
        val (index, v, fines) = first
        val total = fines.fold(BigDecimal.ZERO) { a, p -> a + p.amount }
        val isVan = quote.lowercase().contains("van")
        val who = if (isVan) "Van ${index + 1}, the ${v.shortName} ${v.plate.code} ${v.plate.number}," else "Your ${v.shortName} ${v.plate.code} ${v.plate.number}"
        val what = if (fines.size == 1) "a ${fines[0].title.lowercase()} for ${Money.aed(total, trimWhole = true)}" else "${fines.size} fines, ${Money.aed(total, trimWhole = true)} in all"
        val rows = fines.map { AssistantListRow(it.id, it.symbol, it.hue, it.title, it.detail, Money.aed(it.amount)) }
        val request = if (fines.size == 1) store.paymentRequest(fines[0]).copy(doneByAssistant = true)
        else PaymentRequest(
            id = "assistant-fines-${v.id}", title = "Pay ${fines.size} fines",
            lines = fines.map { PaymentLine(it.title, it.entity, it.amount, it.symbol, it.hue) },
            purpose = PaymentPurpose.Payables(fines.map { it.id }), doneByAssistant = true,
        )
        takeOverTarget = AssistantFollowUp.Push(Route.Vehicle(v.id))
        show(AssistantCard(AssistantText(quote), AssistantText("$who has $what. Pay it now with one confirm?"), surface = AssistantSurface.Rows(rows), controls = AssistantControls.Task), AssistantFaceState.YourTurn)
        val label = Money.aed(total, trimWhole = true)
        yourTurn(AssistantConfirm(AssistantConfirm.Style.Pay(total), AssistantText("Pay $label"), reason = "Pay $label")) { method ->
            finishPayAll(fines, request, method, AssistantIntent.VehicleFines)
        }
    }

    // Faisal · How much power did the café use?
    private fun runEnergy(quote: String) {
        val e = store.state.energy
        if (e == null) {
            show(AssistantCard(AssistantText(quote), AssistantText("I can't see an electricity account yet. Link DEWA in your profile and I'll keep track.")), AssistantFaceState.Awake)
            return
        }
        val q = quote.lowercase()
        val place = if (q.contains("café") || q.contains("cafe")) "The café" else "Your home"
        val change = if (e.changeVsLastMonth == 0) "the same as the month before"
        else "${kotlin.math.abs(e.changeVsLastMonth)}% ${if (e.changeVsLastMonth < 0) "less" else "more"} than the month before"
        val kwh = NumberFormat.getIntegerInstance(Locale.UK).format(e.kWh)
        show(AssistantCard(AssistantText(quote), AssistantText("$place used $kwh kWh in ${e.monthLabel}, $change."), surface = AssistantSurface.Energy(e)), AssistantFaceState.Awake)
        completed = completed + AssistantIntent.Energy.key
    }

    // Wei · When is the next metro?
    private fun runMetro(quote: String) {
        val m = store.state.metro
        val first = m?.departures?.firstOrNull()
        if (m == null || first == null) {
            show(AssistantCard(AssistantText(quote), AssistantText("Add the Metro widget and pick your station, and I'll tell you when the next train leaves.")), AssistantFaceState.Awake)
            return
        }
        val rows = m.departures.map { d ->
            AssistantListRow(d.towards, "tram.fill", Hue.Sky, "Towards ${d.towards}", "${m.line} · then in ${d.nextInMinutes} min", "${d.inMinutes} min")
        }
        show(AssistantCard(
            AssistantText(quote),
            AssistantText("The next ${m.line} train towards ${first.towards} leaves ${m.name} in ${first.inMinutes} minutes."),
            surface = AssistantSurface.Rows(rows),
        ), AssistantFaceState.Awake)
        completed = completed + AssistantIntent.Metro.key
    }

    // On the visa tracker (Paper UR2-0)
    private fun runFees(quote: String) {
        val r = trackerRequest()
        val step = r?.steps?.indexOfFirst { it.title.lowercase().contains("fee") && it.state != RequestStep.State.Done }?.takeIf { it >= 0 }
        if (r == null || step == null) {
            show(AssistantCard(AssistantText(quote), AssistantText("There are no fees left to pay for this.")), AssistantFaceState.Awake)
            return
        }
        val about = r.steps[step].detail.split(" · ").first()
        show(AssistantCard(
            AssistantText(quote),
            AssistantText("$about. You'll see the exact amount before you pay, at step ${step + 1} of ${r.steps.size}."),
            surface = AssistantSurface.Request(r),
        ), AssistantFaceState.Awake)
        completed = completed + AssistantIntent.Fees.key
    }

    private fun runRemind(quote: String) {
        val r = trackerRequest()
        val task = r?.let { req -> store.state.needsYou.firstOrNull { (it.kind as? NeedsYouItem.Kind.RequestStep)?.requestID == req.id } }
        // At the fee step the tracker waits on its current step, with no Needs you item of its own.
        val step = r?.takeIf { it.status is RequestItem.Status.YourTurn }?.currentStepIndex?.let { r.steps[it] }
        val first = store.state.needsYou.firstOrNull()
        val what = when {
            task != null -> AssistantFormat.lowerFirst(task.title)
            r != null && step != null -> "${AssistantFormat.lowerFirst(step.title)} to ${AssistantFormat.lowerFirst(r.title)}"
            // Bills and fines are named as things ("Electricity & water bill"), the rest as things to do.
            first?.payable != null -> "pay your ${AssistantFormat.lowerFirst(first.title)}"
            first != null -> AssistantFormat.lowerFirst(first.title)
            else -> "come back to this"
        }
        show(AssistantCard(
            AssistantText(quote),
            AssistantText("Done. I'll remind you at 20:00 tonight to $what."),
        ), AssistantFaceState.Done)
        completed = completed + AssistantIntent.Remind.key
    }

    private fun runScan(quote: String) {
        // The request waiting on a passport photo: the tracker on screen, or the first one waiting.
        val r = trackerRequest()?.takeIf { waitsOnPassport(it) } ?: store.state.requests.firstOrNull { waitsOnPassport(it) }
        val owner = r?.entity ?: "GDRFA"
        // Start scanning opens the tracker's own fake camera (demo step 7) over the assistant.
        val start = if (r != null) AssistantFollowUp.ScanPassport(r.id)
        else AssistantFollowUp.Present(SheetRoute.ComingSoonSheet(ComingSoon(
            title = "Scan your passport",
            detail = "Scanning with the camera comes with the service flows.",
            owner = owner, symbol = "camera.fill", hue = Hue.Lilac, kind = ComingSoon.Kind.NotInDemo,
        )))
        show(AssistantCard(
            AssistantText(quote),
            AssistantText("Hold your passport flat in good light, photo page up. I'll check it's sharp before it goes to $owner."),
            controls = AssistantControls.Action(AssistantText("Start scanning"), start),
        ), AssistantFaceState.Awake)
    }

    private fun waitsOnPassport(r: RequestItem): Boolean =
        r.status is RequestItem.Status.YourTurn && r.currentStepIndex?.let { r.steps[it].title.lowercase().contains("passport") } == true

    private fun trackerRequest(): RequestItem? {
        val last = router.stack(router.tab).lastOrNull()
        if (last is Route.Request) store.request(last.id)?.let { return it }
        return store.state.requests.firstOrNull { it.status is RequestItem.Status.YourTurn } ?: store.state.requests.firstOrNull()
    }

    // 7 · Wei asks in Mandarin (Paper UXH-0): report a problem with your employer.
    private suspend fun runComplaint(quote: String, mandarin: Boolean) {
        val (m1, m2) = AssistantFormat.lastTwoMonths()
        val name = store.persona?.fullName ?: "Wei Zhang"
        val n1 = m1.monthValue; val n2 = m2.monthValue
        val e1 = AssistantFormat.monthEnglish(m1); val e2 = AssistantFormat.monthEnglish(m2)
        val a1 = AssistantFormat.monthArabic(m1); val a2 = AssistantFormat.monthArabic(m2)
        val year = m2.year
        val fields = listOf(
            AssistantField(0, AssistantText("Name", zh = "姓名"), AssistantText("$name · from UAE PASS", zh = "$name（来自 UAE PASS）")),
            AssistantField(1, AssistantText("Employer", zh = "雇主"), AssistantText("The company on your work permit", zh = "您工作许可上的公司")),
            AssistantField(2, AssistantText("Problem", zh = "问题"), AssistantText("Wages for $e1 and $e2 not paid", zh = "${n1}月和${n2}月的工资未发放")),
            AssistantField(3, AssistantText("Amount", zh = "金额"), AssistantText("AED 6,400", zh = "6,400 迪拉姆")),
        )
        var block = AssistantComplaintBlock(
            fields = fields,
            originalEnglish = "My employer has not paid my wages for $e1 and $e2 $year, a total of AED 6,400. I ask the Permanent Committee for Labour Affairs to look into this.",
            originalArabic = "لم يدفع صاحب العمل راتبي عن شهري $a1 و$a2 $year، بإجمالي 6,400 درهم. أرجو من اللجنة الدائمة لشؤون العمال النظر في ذلك.",
        )
        val quoteText = AssistantText(translation(quote), zh = if (mandarin) quote else null)
        val working = AssistantText(
            "You can report this to Dubai's labour authority, and it stays confidential. I'm preparing it now with your details from UAE PASS.",
            zh = "你可以向迪拜劳工事务部门投诉，他们会保密处理。我正在帮你准备，你的信息会从 UAE PASS 自动填好。",
        )
        takeOverTarget = AssistantFollowUp.OpenService("report-employer-problem")
        show(AssistantCard(quoteText, working, surface = AssistantSurface.Complaint(block), controls = AssistantControls.Task), AssistantFaceState.Working)
        for (i in fields.indices) {
            delay(650)
            block = block.copy(visibleCount = i + 1)
            updateComplaint(block)
            progress = (i + 1).toFloat() / (fields.size + 1)
        }
        delay(400)
        block = block.copy(status = AssistantTaskStatus.YourTurn)
        card?.let {
            card = it.copy(
                headline = AssistantText(
                    "Your report is ready and stays confidential. I'll send it to Dubai's labour authority in English and Arabic.",
                    zh = "投诉已经准备好了，会保密处理。我会用英文和阿拉伯文提交给迪拜劳工事务部门。",
                ),
                surface = AssistantSurface.Complaint(block),
            )
        }
        val readyBlock = block
        yourTurn(AssistantConfirm(
            style = AssistantConfirm.Style.UaePass,
            title = AssistantText("Confirm with UAE PASS", zh = "用 UAE PASS 确认"),
            caption = AssistantText("It's sent in your name", zh = "投诉将以您的名义提交"),
            reason = "Send your report in your name",
        )) { finishComplaint(readyBlock, mandarin) }
    }

    private suspend fun finishComplaint(readyBlock: AssistantComplaintBlock, mandarin: Boolean) {
        var block = readyBlock.copy(status = AssistantTaskStatus.Working)
        updateComplaint(block)
        set(AssistantPhase.Card, AssistantFaceState.Working)
        progress = 0.9f
        delay(900)
        val ref = DemoStore.reference("LAB")
        block = block.copy(status = AssistantTaskStatus.Done, reference = ref)
        store.addRequest(RequestItem(
            id = "assistant-$ref", serviceID = "report-employer-problem",
            title = "Report a problem with your employer", entity = "Labour authority", reference = ref,
            status = RequestItem.Status.InReview(DemoDate.day(5)), notice = null,
            steps = listOf(
                RequestStep("Tell the labour authority what happened", "Today · sent by your assistant in your name", RequestStep.State.Done),
                RequestStep("The labour authority looks into it", "They'll contact you within 5 working days", RequestStep.State.Current),
                RequestStep("You get an answer", "We'll notify you", RequestStep.State.Upcoming),
            ),
            startedOn = LocalDateTime.now(), symbol = "exclamationmark.bubble.fill", hue = Hue.Sand,
        ))
        view?.let { DNHaptics.success(it) }
        // Quiet where it matters: plain words, no celebration, no rating.
        val c = card ?: return
        card = c.copy(
            headline = AssistantText(
                "Sent. Your reference is $ref. The labour authority will contact you within 5 working days.",
                zh = "已提交。参考编号是 $ref。劳工事务部门会在 5 个工作日内联系你。",
            ),
            surface = AssistantSurface.Complaint(block),
            controls = AssistantControls.None,
        )
        progress = 1f
        note = AssistantNote("Report sent", null)
        completed = completed + AssistantIntent.EmployerComplaint(true).key
        lastTopic = "work"
        offersEnglish = mandarin && !showsEnglish
        set(AssistantPhase.Card, AssistantFaceState.Quiet)
        announce(card?.headline?.resolved(showsEnglish) ?: "")
        autoClose()
    }

    private fun translation(quote: String): String = when (quote) {
        WEI_PROMPT -> "My wages haven't been paid for two months. What should I do?"
        "我的公司已经两个月没发工资了，我想投诉。" -> "My company hasn't paid my wages for two months. I want to complain."
        else -> if (AssistantIntent.isMandarin(quote)) "My employer hasn't paid my wages." else quote
    }

    // 8 · Anything else: never a dead end.
    private fun runFallback(quote: String, mandarin: Boolean) {
        show(AssistantCard(
            quote = AssistantText(if (mandarin) "Asked in Mandarin" else quote, zh = if (mandarin) quote else null),
            headline = AssistantText("I can't do that in this demo yet. Try one of these.", zh = if (mandarin) "这个演示里我还做不到。你可以试试下面这些。" else null),
        ), AssistantFaceState.Awake)
    }

    // MARK: Helpers

    private fun show(c: AssistantCard, face: AssistantFaceState) {
        card = c
        set(AssistantPhase.Card, face)
        announce(c.headline.resolved(showsEnglish))
    }

    private fun update(task: AssistantTaskBlock) {
        card?.let { card = it.copy(surface = AssistantSurface.Task(task)) }
    }

    private fun updateComplaint(block: AssistantComplaintBlock) {
        card?.let { card = it.copy(surface = AssistantSurface.Complaint(block)) }
    }

    private fun yourTurn(c: AssistantConfirm, action: suspend (PaymentMethod) -> Unit) {
        onConfirm = action
        confirm = c
        set(AssistantPhase.Card, AssistantFaceState.YourTurn)
        announce("Your turn. ${c.title.resolved(showsEnglish)}.")
        if (AssistantLaunch.autoConfirm || AssistantLaunch.autoStop) {
            scope.launch {
                delay(1500)
                if (AssistantLaunch.autoStop) stop() else confirmTapped()
            }
        }
    }

    private fun finish(headline: AssistantText, surface: AssistantSurface, rating: AssistantRatingPrompt?, note: AssistantNote, intent: AssistantIntent) {
        val c = card ?: return
        card = c.copy(headline = headline, surface = surface, controls = AssistantControls.None, rating = rating)
        progress = 1f
        this.note = note
        completed = completed + intent.key
        lastTopic = intent.topic
        set(AssistantPhase.Card, AssistantFaceState.Done)
        announce(headline.resolved(showsEnglish))
        autoClose()
    }

    private fun autoClose() {
        if (!AssistantLaunch.autoClose) return
        scope.launch {
            delay(2500)
            close()
        }
    }

    private fun set(phase: AssistantPhase, face: AssistantFaceState) {
        this.phase = phase
        this.face = face
    }

    private fun cancelJob() {
        job?.cancel()
        job = null
        isConfirming = false
    }

    companion object {
        const val LISTEN_SEED = "__listen__"
        const val WEI_PROMPT = "我的工资两个月没发了，怎么办？"

        private fun suggestion(text: String): AssistantSuggestion {
            val intent = AssistantIntent.match(text)
            return AssistantSuggestion(text, intent.chipSymbol, intent.chipHue)
        }

        private fun tokens(s: String, mandarin: Boolean): List<String> {
            if (mandarin) return s.codePoints().toArray().map { String(Character.toChars(it)) }
            val words = s.split(" ").filter { it.isNotEmpty() }
            return words.mapIndexed { i, w -> if (i == words.lastIndex) w else "$w " }
        }
    }
}

// MARK: - Store helpers (only read existing state)

/** What renewing a registration takes: the vehicle, the fines that block it, the RTA fee. */
internal data class AssistantRenewalPlan(
    val vehicle: Vehicle,
    val fines: List<Payable>,
    /** RTA renewal fee: AED 400 + AED 20 knowledge and innovation fees. */
    val fee: BigDecimal,
) {
    val finesTotal: BigDecimal get() = fines.fold(BigDecimal.ZERO) { a, p -> a + p.amount }
    val total: BigDecimal get() = fee + finesTotal
}

/** The vehicle whose registration is due (from Needs you, or expiring within 30 days), and its fines. */
internal fun DemoStore.assistantRenewalPlan(): AssistantRenewalPlan? {
    val due = state.needsYou.mapNotNull { item ->
        val k = item.kind
        if (k is NeedsYouItem.Kind.Renewal && k.serviceID == "renew-vehicle-registration") k.thingID else null
    }
    val vehicle = state.vehicles.firstOrNull { it.id in due }
        ?: state.vehicles.filter { DemoDate.daysFromToday(it.registrationExpiry) <= 30 }.minByOrNull { it.registrationExpiry }
        ?: return null
    val fines = payables.filter { it.id in vehicle.fineIDs }
    return AssistantRenewalPlan(vehicle, fines, BigDecimal(420))
}

// MARK: - Debug extras (screenshots and demos)

/**
 * `assistantRun "Renew my car registration"` (or a short key: renew, owe, visa, salik, nol, house, move,
 * cafe, van, energy, metro, needs, fees, remind, scan, wei, unknown; underscores read as spaces) runs a
 * script when the overlay opens; `assistantListen true` starts hold-to-talk; `assistantConfirm true`
 * confirms automatically 1.5 s into your turn; `assistantClose true` closes 2.5 s after done (shows the
 * note); `assistantEnglish true` shows Mandarin cards in English; `assistantStop true` taps Stop 1.5 s
 * into your turn; `assistantPaidFine true` pays the fines blocking the renewal first (the AED 420 path).
 */
internal object AssistantLaunch {
    private var consumed = false

    val autoConfirm: Boolean get() = LaunchOptions.flag("assistantConfirm")
    val autoClose: Boolean get() = LaunchOptions.flag("assistantClose")
    val forceEnglish: Boolean get() = LaunchOptions.flag("assistantEnglish")
    val autoStop: Boolean get() = LaunchOptions.flag("assistantStop")

    data class Start(val run: String?, val listen: Boolean, val payFinesFirst: Boolean = false)

    fun take(): Start {
        if (consumed) return Start(null, false)
        consumed = true
        val raw = LaunchOptions.extras["assistantRun"]?.takeIf { it.isNotBlank() && it != "true" }
        return Start(
            raw?.let { aliases[it.lowercase()] ?: it.replace('_', ' ') },
            LaunchOptions.flag("assistantListen"),
            LaunchOptions.flag("assistantPaidFine"),
        )
    }

    private val aliases = mapOf(
        "renew" to "Renew my car registration",
        "owe" to "How much do I owe?",
        "visa" to "When will my visa renewal be done?",
        "salik" to "Top up Salik with AED 100",
        "nol" to "Top up my nol card with AED 50",
        "house" to "What's next for my house?",
        "move" to "What's next for my move?",
        "cafe" to "Pay everything for the café",
        "van" to "Which van has a fine?",
        "energy" to "How much power did the café use?",
        "metro" to "When is the next metro?",
        "needs" to "What do I need to do?",
        "fees" to "What will the fees be?",
        "remind" to "Remind me tonight at 8",
        "scan" to "Scan my passport with me",
        "wei" to AssistantEngine.WEI_PROMPT,
        "unknown" to "Book me a table for two tonight",
    )
}
