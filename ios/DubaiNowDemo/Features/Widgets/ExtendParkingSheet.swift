import SwiftUI

/// Extend parking — the Parking widget's quick action (Paper C1 · Widget · Quick action).
/// Current session, add-time chips priced at the zone's hourly rate, the new end time, then the
/// fake payment confirmation (built by Activity) applies it.
struct ExtendParkingSheet: View {
    @Environment(DemoStore.self) private var store
    @Environment(Router.self) private var router
    @Environment(\.dismiss) private var dismiss

    @State private var minutes = 60
    @Environment(\.dynamicTypeSize) private var typeSize
    private let options = [30, 60, 120, 180]

    var body: some View {
        NavigationStack {
            Group {
                if let session = store.state.parking {
                    TimelineView(.periodic(from: .now, by: 1)) { context in
                        form(WidgetParkingModel(session: session, now: context.date))
                    }
                } else {
                    notParked
                }
            }
            .navigationTitle("Extend parking")
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

    // MARK: Form

    private func cost(_ session: ParkingSession) -> Decimal {
        (session.ratePerHour * Decimal(minutes) / 60).rounded(scale: 2)
    }

    private func newEnd(_ m: WidgetParkingModel) -> Date {
        max(m.session.endsAt, m.now).addingTimeInterval(TimeInterval(minutes * 60))
    }

    private func form(_ m: WidgetParkingModel) -> some View {
        let price = cost(m.session)
        let end = newEnd(m)
        return ScrollView {
            VStack(alignment: .leading, spacing: 22) {
                sessionCard(m)

                VStack(alignment: .leading, spacing: 10) {
                    Text("Add time".uppercased())
                        .dnFont(.overline)
                        .foregroundStyle(DNColor.textGroupedHeader)
                        .accessibilityAddTraits(.isHeader)
                    ViewThatFits(in: .horizontal) {
                        HStack(spacing: DNSpace.chipGap) { chipRow(options) }
                        VStack(spacing: DNSpace.chipGap) {
                            HStack(spacing: DNSpace.chipGap) { chipRow(Array(options.prefix(2))) }
                            HStack(spacing: DNSpace.chipGap) { chipRow(Array(options.suffix(2))) }
                        }
                    }
                }

                let resultLayout = typeSize.isAccessibilitySize
                    ? AnyLayout(VStackLayout(alignment: .leading, spacing: 14))
                    : AnyLayout(HStackLayout(alignment: .bottom))
                resultLayout {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("New end time")
                            .dnFont(.subheadline)
                            .foregroundStyle(DNColor.textSecondary)
                        Text(DemoDate.time(end))
                            .dnFont(.numericLarge)
                            .foregroundStyle(DNColor.textPrimary)
                            .contentTransition(.numericText())
                    }
                    .accessibilityElement(children: .combine)
                    if !typeSize.isAccessibilitySize { Spacer(minLength: 12) }
                    VStack(alignment: typeSize.isAccessibilitySize ? .leading : .trailing, spacing: 2) {
                        Text("Cost")
                            .dnFont(.subheadline)
                            .foregroundStyle(DNColor.textSecondary)
                        MoneyText(amount: price, size: .numericLarge)
                            .contentTransition(.numericText())
                    }
                    .accessibilityElement(children: .combine)
                }
                .animation(DNMotion.resolve(DNMotion.smooth), value: minutes)

                VStack(spacing: 12) {
                    Button("Extend for \(Money.aed(price))") { pay(m, price: price, end: end) }
                        .buttonStyle(.dn(.primary))
                    Text("Run by Parkin · we'll remind you 10 min before it ends")
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
    }

    private func sessionCard(_ m: WidgetParkingModel) -> some View {
        HStack(spacing: 12) {
            WidgetParkingSign(size: 40)
            VStack(alignment: .leading, spacing: 2) {
                Text("Zone \(m.session.zone) · \(m.session.area)")
                    .dnFont(.headline)
                    .foregroundStyle(DNColor.textPrimary)
                Group {
                    switch m.phase {
                    case .expired:
                        Text("\(m.session.plate.code) \(m.session.plate.number) · ended \(m.ends) (\(m.minutesOver) min ago)")
                            .foregroundStyle(DNColor.statusErrorForeground)
                    case .endingSoon:
                        Text("\(m.session.plate.code) \(m.session.plate.number) · ends \(m.ends) (\(m.minutesLeft) min)")
                            .foregroundStyle(DNColor.statusWarningForeground)
                    case .live:
                        Text("\(m.session.plate.code) \(m.session.plate.number) · ends \(m.ends) (\(minutesText(m.minutesLeft)))")
                            .foregroundStyle(DNColor.textSecondary)
                    }
                }
                .dnFont(.subheadline)
                .contentTransition(.numericText())
            }
            Spacer(minLength: 0)
        }
        .padding(14)
        .background(DNColor.surfaceSecondary, in: .rect(cornerRadius: DNRadius.card, style: .continuous))
        .accessibilityElement(children: .combine)
    }

    @ViewBuilder
    private func chipRow(_ values: [Int]) -> some View {
        ForEach(values, id: \.self) { value in
            let selected = minutes == value
            Button {
                DNHaptics.selection()
                withAnimation(DNMotion.resolve(DNMotion.snappy)) { minutes = value }
            } label: {
                Text(label(value))
                    .dnFont(.subheadline)
                    .fontWeight(selected ? .semibold : .medium)
                    .lineLimit(1)
                    .fixedSize()
                    .foregroundStyle(selected ? DNColor.chipTextSelected : DNColor.chipText)
                    .padding(.horizontal, 12)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 6)
                    .frame(minHeight: DNSpace.chipHeight)
                    .background(selected ? DNColor.chipSurfaceSelected : DNColor.chipSurfaceOnCard, in: .capsule)
                    .contentShape(.capsule)
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Add \(label(value))")
            .accessibilityAddTraits(selected ? .isSelected : [])
        }
    }

    private func label(_ m: Int) -> String {
        switch m {
        case ..<60: "\(m) min"
        case 60: "1 hour"
        default: m % 60 == 0 ? "\(m / 60) hours" : "\(m / 60) h \(m % 60) min"
        }
    }

    private func minutesText(_ m: Int) -> String {
        m >= 60 ? "\(m / 60) h \(m % 60) min" : "\(m) min"
    }

    private func pay(_ m: WidgetParkingModel, price: Decimal, end: Date) {
        let request = PaymentRequest(
            id: "extend-parking-\(minutes)-\(Int(Date.now.timeIntervalSince1970))",
            title: "Extend parking",
            lines: [PaymentLine(
                title: "Parking · Zone \(m.session.zone)",
                detail: "\(label(minutes)) more · until \(DemoDate.time(end))",
                amount: price,
                symbol: "p.square.fill",
                hue: .sky
            )],
            purpose: .extendParking(minutes: minutes)
        )
        router.present(.payment(request))
    }

    // MARK: Not parked

    private var notParked: some View {
        VStack(spacing: 16) {
            Spacer(minLength: 12)
            WidgetParkingSign(size: 56, dimmed: true)
            VStack(spacing: 8) {
                Text("You're not parked right now")
                    .dnFont(.title3)
                    .foregroundStyle(DNColor.textPrimary)
                Text("Start a session when you park. You can extend it from here or from the Parking widget on Home.")
                    .dnFont(.body)
                    .foregroundStyle(DNColor.textSecondary)
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Spacer(minLength: 12)
            Button("Start parking") {
                router.present(.comingSoon(ComingSoon(
                    title: "Pay for parking", owner: "Parkin", symbol: "p.square.fill", hue: .sky, kind: .servicesPhase
                )))
            }
            .buttonStyle(.dn(.primary))
        }
        .padding(.horizontal, DNSpace.insetPage)
        .padding(.bottom, 16)
    }
}
