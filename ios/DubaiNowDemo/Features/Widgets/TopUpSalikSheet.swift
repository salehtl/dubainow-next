import SwiftUI

/// Top up Salik — the Salik widget's quick action. Balance, amount chips (AED 50, 100, 200, Other),
/// the new balance, then the fake payment confirmation applies it.
struct TopUpSalikSheet: View {
    @Environment(DemoStore.self) private var store
    @Environment(Router.self) private var router
    @Environment(\.dismiss) private var dismiss

    private enum Choice: Hashable {
        case preset(Int)
        case other
    }

    @State private var choice: Choice = .preset(100)
    @State private var otherText = ""
    @FocusState private var otherFocused: Bool
    @Environment(\.dynamicTypeSize) private var typeSize

    private let presets = [50, 100, 200]
    private let minimum: Decimal = 50
    private let maximum: Decimal = 2_000

    var body: some View {
        NavigationStack {
            Group {
                if store.widgetSalikLinked { form } else { notLinked }
            }
            .navigationTitle("Top up Salik")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(role: .close) { dismiss() }.tint(DNColor.textPrimary)
                        .tint(DNColor.textPrimary)
                        .accessibilityLabel("Close")
                }
            }
        }
        .presentationDetents([.fraction(0.68), .large])
        .presentationDragIndicator(.visible)
    }

    // MARK: Amount

    private var amount: Decimal? {
        switch choice {
        case .preset(let n): return Decimal(n)
        case .other:
            let cleaned = otherText.replacingOccurrences(of: ",", with: "").trimmingCharacters(in: .whitespaces)
            guard let d = Decimal(string: cleaned), d >= minimum, d <= maximum else { return nil }
            return d.rounded(scale: 2)
        }
    }

    private var otherHint: String? {
        guard choice == .other else { return nil }
        let cleaned = otherText.replacingOccurrences(of: ",", with: "")
        guard !cleaned.isEmpty, let d = Decimal(string: cleaned) else { return "Top-ups start at AED 50" }
        if d < minimum { return "Top-ups start at AED 50" }
        if d > maximum { return "You can top up up to AED 2,000 at a time" }
        return nil
    }

    // MARK: Form

    private var form: some View {
        let balance = store.state.salikBalance
        let now = WidgetSalikModel(balance: balance)
        let after = amount.map { WidgetSalikModel(balance: balance + $0) }
        return ScrollView {
            VStack(alignment: .leading, spacing: 22) {
                // Balance, drawn like the gantry widget.
                VStack(alignment: .leading, spacing: 10) {
                    HStack(spacing: 10) {
                        WidgetGlyph(symbol: "road.lanes", ink: DNColor.widgetOnInverse, fill: DNColor.widgetOnInverseRule)
                        Text("Your Salik balance")
                            .dnFont(.headline)
                            .foregroundStyle(DNColor.widgetOnInverse)
                    }
                    let balanceLayout = typeSize.isAccessibilitySize
                        ? AnyLayout(VStackLayout(alignment: .leading, spacing: 6))
                        : AnyLayout(HStackLayout(alignment: .firstTextBaseline))
                    balanceLayout {
                        HStack(alignment: .firstTextBaseline, spacing: 5) {
                            Text("AED")
                                .dnFont(.numericUnit)
                                .foregroundStyle(DNColor.widgetOnInverseSecondary)
                            Text(Money.figure(balance, trimWhole: true))
                                .dnFont(.numericLarge)
                                .foregroundStyle(DNColor.widgetOnInverse)
                                .contentTransition(.numericText())
                        }
                        if !typeSize.isAccessibilitySize { Spacer(minLength: 8) }
                        HStack(spacing: 6) {
                            Circle().fill(now.isLow ? DNColor.signLow : DNColor.signLive).frame(width: 7, height: 7)
                            Text(now.tripsLine)
                                .dnFont(.subheadline)
                                .fontWeight(.semibold)
                                .foregroundStyle(now.isLow ? DNColor.signLow : DNColor.widgetOnInverseSecondary)
                        }
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(16)
                .background(DNColor.widgetSurfaceInverse, in: .rect(cornerRadius: DNRadius.card, style: .continuous))
                .accessibilityElement(children: .combine)

                VStack(alignment: .leading, spacing: 10) {
                    Text("Amount".uppercased())
                        .dnFont(.overline)
                        .foregroundStyle(DNColor.textGroupedHeader)
                        .accessibilityAddTraits(.isHeader)
                    ViewThatFits(in: .horizontal) {
                        HStack(spacing: DNSpace.chipGap) {
                            ForEach(presets, id: \.self) { n in chip("AED \(n)", choice: .preset(n)) }
                            chip("Other", choice: .other)
                        }
                        VStack(spacing: DNSpace.chipGap) {
                            HStack(spacing: DNSpace.chipGap) {
                                chip("AED 50", choice: .preset(50))
                                chip("AED 100", choice: .preset(100))
                            }
                            HStack(spacing: DNSpace.chipGap) {
                                chip("AED 200", choice: .preset(200))
                                chip("Other", choice: .other)
                            }
                        }
                    }
                    if choice == .other {
                        VStack(alignment: .leading, spacing: DNSpace.fieldLabelGap) {
                            HStack(spacing: 6) {
                                Text("AED")
                                    .dnFont(.numericUnit)
                                    .foregroundStyle(DNColor.textSecondary)
                                TextField("Amount", text: $otherText)
                                    .keyboardType(.decimalPad)
                                    .focused($otherFocused)
                                    .font(.system(size: 17, weight: .semibold, design: .rounded))
                                    .foregroundStyle(DNColor.fieldText)
                                    .accessibilityLabel("Amount in dirhams")
                            }
                            .padding(.horizontal, DNSpace.fieldInsetInline)
                            .frame(minHeight: DNSpace.fieldMinHeight)
                            .background(DNColor.fieldSurface, in: .rect(cornerRadius: DNRadius.field, style: .continuous))
                            if let hint = otherHint {
                                Text(hint)
                                    .dnFont(.footnote)
                                    .foregroundStyle(DNColor.fieldHint)
                            }
                        }
                        .transition(.opacity.combined(with: .move(edge: .top)))
                    }
                }

                let resultLayout = typeSize.isAccessibilitySize
                    ? AnyLayout(VStackLayout(alignment: .leading, spacing: 6))
                    : AnyLayout(HStackLayout(alignment: .bottom))
                resultLayout {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("New balance")
                            .dnFont(.subheadline)
                            .foregroundStyle(DNColor.textSecondary)
                        if let after {
                            MoneyText(amount: after.balance, size: .numericLarge)
                                .contentTransition(.numericText())
                        } else {
                            Text("–").dnFont(.numericLarge).foregroundStyle(DNColor.textSecondary)
                        }
                    }
                    if !typeSize.isAccessibilitySize { Spacer(minLength: 12) }
                    if let after {
                        Text(after.tripsLine)
                            .dnFont(.subheadline)
                            .foregroundStyle(DNColor.textSecondary)
                            .padding(.bottom, 6)
                    }
                }
                .accessibilityElement(children: .combine)
                .animation(DNMotion.resolve(DNMotion.smooth), value: amount)

                VStack(spacing: 12) {
                    Button(amount.map { "Pay \(Money.aed($0, trimWhole: true))" } ?? "Pay") { pay() }
                        .buttonStyle(.dn(.primary))
                        .disabled(amount == nil)
                    Text("Run by Salik · your new balance shows straight away")
                        .dnFont(.footnote)
                        .foregroundStyle(DNColor.textSecondary)
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: .infinity)
                }
            }
            .padding(.horizontal, DNSpace.insetPage)
            .padding(.top, 8)
            .padding(.bottom, 16)
        }
        .scrollBounceBehavior(.basedOnSize)
        .scrollDismissesKeyboard(.interactively)
    }

    private func chip(_ title: String, choice value: Choice) -> some View {
        let selected = choice == value
        return Button {
            DNHaptics.selection()
            withAnimation(DNMotion.resolve(DNMotion.snappy)) { choice = value }
            otherFocused = value == .other
        } label: {
            Text(title)
                .dnFont(.subheadline)
                    .fontWeight(selected ? .semibold : .medium)
                .lineLimit(1)
                .minimumScaleFactor(0.85)
                .foregroundStyle(selected ? DNColor.chipTextSelected : DNColor.chipText)
                .padding(.horizontal, 10)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 6)
                .frame(minHeight: DNSpace.chipHeight)
                .background(selected ? DNColor.chipSurfaceSelected : DNColor.chipSurfaceOnCard, in: .capsule)
                .contentShape(.capsule)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(title == "Other" ? "Other amount" : title)
        .accessibilityAddTraits(selected ? .isSelected : [])
    }

    private func pay() {
        guard let amount else { return }
        otherFocused = false
        let request = PaymentRequest(
            id: "salik-topup-\(Int(Date.now.timeIntervalSince1970))",
            title: "Top up Salik",
            lines: [PaymentLine(
                title: "Salik top-up",
                detail: "New balance \(Money.aed(store.state.salikBalance + amount, trimWhole: true))",
                amount: amount,
                symbol: "road.lanes",
                hue: .sand
            )],
            purpose: .salikTopUp(amount)
        )
        router.present(.payment(request))
    }

    // MARK: Not linked

    private var notLinked: some View {
        VStack(spacing: 16) {
            Spacer(minLength: 12)
            WidgetGlyph(symbol: "road.lanes", ink: DNColor.widgetOnInverse, fill: DNColor.widgetSurfaceInverse, size: 56)
            VStack(spacing: 8) {
                Text("Link your Salik account")
                    .dnFont(.title3)
                    .foregroundStyle(DNColor.textPrimary)
                Text("Once it's linked, your balance shows on Home and you can top up in two taps.")
                    .dnFont(.body)
                    .foregroundStyle(DNColor.textSecondary)
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Spacer(minLength: 12)
            Button("Link Salik") {
                router.present(.comingSoon(ComingSoon(
                    title: "Link your Salik account",
                    detail: "In the full app you confirm the link once with UAE PASS.",
                    owner: "Salik", symbol: "link", hue: .sand, kind: .notInDemo
                )))
            }
            .buttonStyle(.dn(.primary))
        }
        .padding(.horizontal, DNSpace.insetPage)
        .padding(.bottom, 16)
    }
}
