import LocalAuthentication
import SwiftUI

/// The fake payment confirmation used everywhere (`router.present(.payment(request))`).
/// Check what you're paying → choose how → "Pay AED x" with Face ID (LocalAuthentication) →
/// the sheet becomes the receipt: the tick draws in, rows arrive one after another, success haptic.
/// No money moves; the sheet says so.
struct PaymentSheet: View {
    var request: PaymentRequest

    @Environment(DemoStore.self) private var store
    @Environment(\.dismiss) private var dismiss
    @State private var phase: PaymentPhase = .review
    @State private var method: PaymentMethod = .applePay
    @State private var detent: PresentationDetent
    @State private var notice: String?

    enum PaymentPhase: Equatable {
        case review
        case confirming
        case paid(receiptID: String)
    }

    init(request: PaymentRequest) {
        self.request = request
        _detent = State(initialValue: request.lines.count > 1 ? .large : .medium)
    }

    var body: some View {
        NavigationStack {
            Group {
                switch phase {
                case .paid(let receiptID):
                    paid(receiptID)
                        .transition(.blurReplace)
                case .review, .confirming:
                    if isNothingToPay {
                        nothingToPay
                    } else {
                        review
                            .transition(.blurReplace)
                    }
                }
            }
            .navigationTitle(navigationTitle)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(role: .close) { dismiss() }.tint(DNColor.textPrimary)
                        .disabled(phase == .confirming)
                }
                if case .paid(let receiptID) = phase {
                    ToolbarItem(placement: .topBarTrailing) {
                        ActivityReceiptShareButton(receiptID: receiptID)
                    }
                }
            }
        }
        .presentationDetents([.medium, .large], selection: $detent)
        .presentationDragIndicator(.visible)
        .interactiveDismissDisabled(phase == .confirming)
        .onAppear {
            if let first = store.state.paymentMethods.first, !store.state.paymentMethods.contains(method) {
                method = first
            }
        }
        .task {
            guard ActivityDemoArg.value == "pay" || ActivityDemoArg.value == "fees" else { return }
            try? await Task.sleep(for: .seconds(1))
            pay()
        }
    }

    private var navigationTitle: String {
        if case .paid = phase { return "Receipt" }
        return isNothingToPay ? "" : "Check and pay"
    }

    // MARK: Guards

    private var isAlreadyPaid: Bool {
        guard case .payables(let ids) = request.purpose, !ids.isEmpty else { return false }
        return !ids.contains { id in store.payables.contains { $0.id == id } }
    }

    private var isNothingToPay: Bool {
        request.lines.isEmpty || request.total <= 0 || isAlreadyPaid
    }

    private var nothingToPay: some View {
        ContentUnavailableView {
            Label("Nothing to pay", systemImage: "checkmark.circle")
        } description: {
            Text(isAlreadyPaid
                 ? "This has already been paid. The receipt is in Activity, under History."
                 : "There's nothing due here right now.")
        } actions: {
            Button("Done") { dismiss() }
                .buttonStyle(.dn(.secondary, size: .medium, fullWidth: false))
        }
    }

    // MARK: Review

    private var review: some View {
        List {
            Section {
                ForEach(Array(request.lines.enumerated()), id: \.offset) { _, line in
                    PaymentLineRow(line: line)
                }
            } header: {
                Text(payeeLine)
                    .textCase(nil)
            }

            Section {
                PaymentTotalRow(total: request.total)
            }

            Section {
                payWith
            }
        }
        .listStyle(.insetGrouped)
        .listSectionSpacing(16)
        .safeAreaBar(edge: .bottom) {
            payBar
        }
    }

    private var payeeLine: String {
        let payees = request.activityPayees
        if request.lines.count == 1 { return "You're paying \(payees)" }
        return "\(request.lines.count) payments to \(payees)"
    }

    private var payWith: some View {
        let methods = store.state.paymentMethods.isEmpty ? [PaymentMethod.applePay] : store.state.paymentMethods
        return Menu {
            Picker("Pay with", selection: $method) {
                ForEach(methods, id: \.self) { m in
                    Label(m.label, systemImage: m.activitySymbol).tag(m)
                }
            }
        } label: {
            HStack(spacing: 12) {
                DNRowIcon(symbol: method.activitySymbol, color: DNColor.textPrimary)
                VStack(alignment: .leading, spacing: 1) {
                    Text("Pay with")
                        .dnFont(.footnote)
                        .foregroundStyle(DNColor.textSecondary)
                    Text(method.label)
                        .dnFont(.rowTitle)
                        .foregroundStyle(DNColor.textPrimary)
                }
                Spacer(minLength: 8)
                if methods.count > 1 {
                    Text("Change")
                        .dnFont(.subheadline)
                        .foregroundStyle(DNColor.textLink)
                }
            }
            .padding(.vertical, 2)
            .contentShape(.rect)
        }
        .disabled(methods.count < 2 || phase == .confirming)
        .accessibilityLabel("Pay with \(method.label)")
        .accessibilityHint(methods.count > 1 ? "Choose another way to pay" : "")
    }

    private var payBar: some View {
        VStack(spacing: 10) {
            if let notice {
                Text(notice)
                    .dnFont(.footnote)
                    .fontWeight(.medium)
                    .foregroundStyle(DNColor.statusErrorForeground)
                    .transition(.opacity)
            }
            Button(action: pay) {
                HStack(spacing: 10) {
                    if phase == .confirming {
                        ProgressView()
                            .tint(DNColor.textOnAccent)
                        Text("Confirming…")
                    } else {
                        Image(systemName: "faceid")
                            .font(.system(size: 20, weight: .medium))
                            .accessibilityHidden(true)
                        Text("Pay \(Money.aed(request.total))")
                    }
                }
            }
            .buttonStyle(.dn(.primary))
            .allowsHitTesting(phase == .review)
            .accessibilityLabel(phase == .confirming ? "Confirming payment" : "Pay \(Money.spoken(request.total))")
            .accessibilityHint("Confirms with Face ID or your passcode")

            Label("Demo · no money moves", systemImage: "info.circle")
                .dnFont(.caption1)
                .foregroundStyle(DNColor.textSecondary)
        }
        .padding(.horizontal, DNSpace.sheetPadding)
        .padding(.top, 10)
        .padding(.bottom, 6)
    }

    // MARK: Confirm (Face ID / passcode; a short "Confirming…" when neither is set up)

    private func pay() {
        guard phase == .review else { return }
        withAnimation(DNMotion.resolve(DNMotion.snappy)) {
            notice = nil
            phase = .confirming
        }
        let context = LAContext()
        var error: NSError?
        // Face ID (with passcode as its fallback) only when Face ID is set up; a Simulator
        // without enrolled Face ID gets the short "Confirming…" instead of a passcode prompt.
        guard context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error),
              context.canEvaluatePolicy(.deviceOwnerAuthentication, error: &error) else {
            Task {
                try? await Task.sleep(for: .milliseconds(900))
                complete()
            }
            return
        }
        Task {
            do {
                let ok = try await context.evaluatePolicy(.deviceOwnerAuthentication, localizedReason: "Confirm your payment")
                if ok { complete() } else { backToReview(nil) }
            } catch let error as LAError {
                switch error.code {
                case .userCancel, .appCancel, .systemCancel:
                    backToReview(nil)
                case .authenticationFailed:
                    DNHaptics.error()
                    backToReview("That didn't match. Try again.")
                default:
                    // Not enrolled, locked out, no passcode… never block the demo.
                    try? await Task.sleep(for: .milliseconds(900))
                    complete()
                }
            } catch {
                try? await Task.sleep(for: .milliseconds(900))
                complete()
            }
        }
    }

    private func backToReview(_ message: String?) {
        withAnimation(DNMotion.resolve(DNMotion.snappy)) {
            phase = .review
            notice = message
        }
    }

    private func complete() {
        guard phase == .confirming else { return }
        let receipt = store.completePayment(request, method: method)
        if request.lines.count > 1 || isFees {
            store.activityAttachRows(
                request.lines.map { ReceiptRow(label: $0.title, value: Money.aed($0.amount)) },
                toReceipt: receipt.id
            )
        }
        DNHaptics.success()
        withAnimation(DNMotion.resolve(DNMotion.smooth)) {
            phase = .paid(receiptID: receipt.id)
            detent = .large
        }
    }

    private var isFees: Bool {
        if case .requestFees = request.purpose { return true }
        return false
    }

    // MARK: Paid

    private func paid(_ receiptID: String) -> some View {
        ActivityReceiptList(receiptID: receiptID, animateIn: true)
            .safeAreaBar(edge: .bottom) {
                Button("Done") { dismiss() }
                    .buttonStyle(.dn(.primary))
                    .padding(.horizontal, DNSpace.sheetPadding)
                    .padding(.top, 10)
                    .padding(.bottom, 6)
            }
    }
}

// MARK: - Rows

/// One thing being paid: hue icon · title · who it goes to · amount.
struct PaymentLineRow: View {
    let line: PaymentLine
    @Environment(\.dynamicTypeSize) private var typeSize

    var body: some View {
        let layout = typeSize.isAccessibilitySize
            ? AnyLayout(VStackLayout(alignment: .leading, spacing: 6))
            : AnyLayout(HStackLayout(spacing: 12))
        layout {
            HueIcon(symbol: line.symbol, hue: line.hue, size: 40)
            VStack(alignment: .leading, spacing: 2) {
                Text(line.title)
                    .dnFont(.rowTitle)
                    .foregroundStyle(DNColor.textPrimary)
                    .fixedSize(horizontal: false, vertical: true)
                Text(line.detail)
                    .dnFont(.footnote)
                    .foregroundStyle(DNColor.textSecondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            Text(Money.aed(line.amount))
                .dnFont(.numericUnit)
                .foregroundStyle(DNColor.textPrimary)
                .lineLimit(1)
                .fixedSize()
        }
        .padding(.vertical, 4)
        .accessibilityElement(children: .combine)
    }
}

struct PaymentTotalRow: View {
    let total: Decimal

    var body: some View {
        ViewThatFits(in: .horizontal) {
            HStack(alignment: .firstTextBaseline) {
                Text("Total")
                    .dnFont(.headline)
                    .foregroundStyle(DNColor.textPrimary)
                Spacer(minLength: 12)
                MoneyText(amount: total, size: .numericMedium)
            }
            VStack(alignment: .leading, spacing: 4) {
                Text("Total")
                    .dnFont(.headline)
                    .foregroundStyle(DNColor.textPrimary)
                MoneyText(amount: total, size: .numericMedium)
            }
        }
        .padding(.vertical, 6)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Total, \(Money.spoken(total))")
    }
}

private extension PaymentRequest {
    /// Who the money goes to. A top-up or a parking extension shows the new balance or end time
    /// as its line detail, so those name the payee instead.
    var activityPayees: String {
        switch purpose {
        case .salikTopUp: "Salik"
        case .extendParking: "Parkin"
        default: ActivityFormat.list(lines.map(\.detail))
        }
    }
}
