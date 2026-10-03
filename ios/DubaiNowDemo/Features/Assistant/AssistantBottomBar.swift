import SwiftUI

/// Your side (the bottom): up to three suggestions above the ask bar; while it listens, the
/// transcript; when it needs you, the one confirm under your thumb (Paper RUW-0, SVR-0, TQX-0, UXH-0).
struct AssistantBottomBar: View {
    @Bindable var engine: AssistantEngine
    var suggestions: [AssistantSuggestion]
    var fieldFocus: FocusState<Bool>.Binding

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            above
            bar
        }
    }

    // MARK: Above the bar

    @ViewBuilder private var above: some View {
        if engine.phase == .listening {
            transcript
                .transition(.opacity)
        } else if let confirm = engine.confirm {
            caption(confirm)
                .transition(.opacity)
        } else if !suggestions.isEmpty {
            VStack(alignment: .leading, spacing: 8) {
                ForEach(Array(suggestions.enumerated()), id: \.element.id) { i, s in
                    AssistantChip(suggestion: s) {
                        DNHaptics.selection()
                        engine.run(s.text)
                    }
                    .transition(.asymmetric(
                        insertion: .opacity.combined(with: .offset(y: 12)).animation(DNMotion.resolve(DNMotion.smooth).delay(DNMotion.stagger(i))),
                        removal: .opacity
                    ))
                }
            }
            .accessibilityElement(children: .contain)
            .accessibilityLabel("Suggestions")
        }
    }

    private var transcript: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("\(Text(engine.heard).foregroundStyle(DNColor.textPrimary))\(Text(engine.hearing).foregroundStyle(DNColor.neutral400))")
                .dnFont(.title1)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)
                .accessibilityLabel(engine.heard.isEmpty ? "Listening" : engine.heard)
            HStack(spacing: 8) {
                Circle()
                    .fill(DNColor.actionAccent)
                    .frame(width: 8, height: 8)
                    .background(Circle().fill(DNColor.actionAccent.opacity(0.18)).frame(width: 16, height: 16))
                Text("Listening · let go when you're done")
                    .dnFont(.subheadline)
                    .fontWeight(.medium)
                    .foregroundStyle(DNColor.textSecondary)
            }
            .accessibilityElement(children: .combine)
        }
        .padding(.horizontal, 4)
        .padding(.bottom, 6)
    }

    @ViewBuilder private func caption(_ confirm: AssistantConfirm) -> some View {
        HStack(spacing: 6) {
            switch confirm.style {
            case .pay:
                Text("With \(engine.paymentMethod.label) ·")
                    .foregroundStyle(DNColor.textSecondary)
                    .fontWeight(.medium)
                Button("Change") { engine.changeMethod() }
                    .fontWeight(.semibold)
                    .foregroundStyle(DNColor.textLink)
                    .buttonStyle(.plain)
                    .frame(minHeight: 44)
                    .padding(.vertical, -12)
                    .contentShape(.rect)
                    .disabled(!engine.canChangeMethod)
                    .accessibilityLabel("Change how you pay")
                    .accessibilityValue(engine.paymentMethod.label)
            case .uaePass:
                Text(confirm.caption?.resolved(english: engine.showsEnglish) ?? "")
                    .foregroundStyle(DNColor.textSecondary)
            }
        }
        .dnFont(.subheadline)
        .frame(maxWidth: .infinity)
        .padding(.leading, 56)
    }

    // MARK: The bar

    private var bar: some View {
        HStack(spacing: 10) {
            GlassIconButton(symbol: "xmark", label: engine.confirm == nil ? "Close the assistant" : "Not now", size: 56) {
                engine.close()
            }
            .accessibilityHint(engine.confirm == nil ? "You're back where you were" : "Nothing is paid or sent")

            if let confirm = engine.confirm {
                AssistantConfirmButton(confirm: confirm, english: engine.showsEnglish, isConfirming: engine.isConfirming) {
                    engine.confirmTapped()
                }
                .transition(.opacity.combined(with: .scale(scale: 0.96)))
            } else {
                askBar
                    .transition(.opacity)
            }
        }
        .gesture(
            DragGesture(minimumDistance: 20).onEnded { value in
                if value.translation.height > 60 { engine.close() }
            }
        )
    }

    private var askBar: some View {
        HStack(spacing: 8) {
            if engine.phase == .listening {
                AssistantWaveform(progress: engine.heard.count, total: engine.heard.count + engine.hearing.count)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .accessibilityHidden(true)
                Button {
                    engine.sendListening()
                } label: {
                    roundIcon("arrow.up")
                }
                .buttonStyle(.dnPressable)
                .accessibilityLabel("Send")
            } else {
                TextField(placeholder, text: $engine.draft)
                    .dnFont(.body)
                    .foregroundStyle(DNColor.textPrimary)
                    .focused(fieldFocus)
                    .submitLabel(.send)
                    .onSubmit { engine.sendDraft() }
                    .frame(maxWidth: .infinity, minHeight: 44)
                if engine.draft.trimmingCharacters(in: .whitespaces).isEmpty {
                    Button {
                        fieldFocus.wrappedValue = false
                        engine.listen()
                    } label: {
                        roundIcon("mic.fill")
                    }
                    .buttonStyle(.dnPressable)
                    .accessibilityLabel("Talk")
                    .accessibilityHint("Say what you need")
                } else {
                    Button {
                        fieldFocus.wrappedValue = false
                        engine.sendDraft()
                    } label: {
                        roundIcon("arrow.up")
                    }
                    .buttonStyle(.dnPressable)
                    .accessibilityLabel("Send")
                }
            }
        }
        .padding(.leading, 20)
        .padding(.trailing, 8)
        .frame(minHeight: 56)
        .background(DNColor.white.opacity(0.4), in: .capsule)
        .glassEffect(.regular.tint(DNColor.white.opacity(0.4)), in: .capsule)
    }

    /// Idle: "Ask or tell me what to do". While it's working: "Ask or add something".
    private var placeholder: String {
        engine.card == nil ? "Ask or tell me what to do" : "Ask or add something"
    }

    private func roundIcon(_ symbol: String) -> some View {
        Image(systemName: symbol)
            .font(.system(size: 17, weight: .semibold))
            .foregroundStyle(DNColor.textOnAccent)
            .frame(width: 40, height: 40)
            .background(DNColor.actionAccent, in: .circle)
            .shadow(color: DNColor.actionAccent.opacity(0.3), radius: 4, y: 2)
            .frame(width: 44, height: 44)
            .contentShape(.circle)
    }
}

/// A suggestion: a small glass capsule with a glyph in the topic's ink.
private struct AssistantChip: View {
    var suggestion: AssistantSuggestion
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                Image(systemName: suggestion.symbol)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(suggestion.hue.ink)
                    .frame(width: 18)
                Text(suggestion.text)
                    .dnFont(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundStyle(DNColor.textPrimary)
                    .lineLimit(2)
            }
            .padding(.leading, 12)
            .padding(.trailing, 16)
            .frame(minHeight: 44)
            .background(DNColor.white.opacity(0.55), in: .capsule)
            .glassEffect(.regular.tint(DNColor.white.opacity(0.4)).interactive(), in: .capsule)
            .contentShape(.capsule)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(suggestion.text)
        .accessibilityHint("Asks the assistant")
    }
}

/// The confirm: "Pay AED 1,020" with the Face ID glyph, or "Confirm with UAE PASS" (black, Touch ID glyph).
struct AssistantConfirmButton: View {
    var confirm: AssistantConfirm
    var english: Bool
    var isConfirming: Bool
    var action: () -> Void

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        Button(action: action) {
            HStack(spacing: 10) {
                Image(systemName: glyph)
                    .font(.system(size: isUAEPass ? 22 : 19, weight: .medium))
                    .symbolEffect(.pulse, options: .repeating, isActive: isConfirming && !reduceMotion)
                Text(isConfirming ? (english ? "Confirming…" : "确认中…") : confirm.title.resolved(english: english))
                    .dnFont(.button)
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
                    .contentTransition(.opacity)
            }
            .foregroundStyle(DNColor.white)
            .frame(maxWidth: .infinity)
            .frame(minHeight: 56)
            .background(background, in: .capsule)
            .overlay(Capsule().strokeBorder(DNColor.white.opacity(0.25), lineWidth: 0.5))
            .shadow(color: (isUAEPass ? DNColor.ink950 : DNColor.actionAccent).opacity(0.3), radius: 11, y: 8)
            .contentShape(.capsule)
        }
        .buttonStyle(.dnPressable)
        .disabled(isConfirming)
        .accessibilityLabel(confirm.title.resolved(english: english))
        .accessibilityHint(isUAEPass ? "Confirms with UAE PASS. It's sent in your name." : "Confirms with Face ID")
    }

    private var isUAEPass: Bool { confirm.style == .uaePass }
    private var glyph: String { isUAEPass ? "touchid" : "faceid" }

    private var background: some ShapeStyle {
        if isUAEPass { return AnyShapeStyle(DNColor.ink950) }
        return AnyShapeStyle(DNColor.actionAccent.gradient)
    }
}

/// Listening: the voice as bars; heard bars in the accent, the rest as dots.
private struct AssistantWaveform: View {
    var progress: Int
    var total: Int
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private let heights: [CGFloat] = [4, 8, 14, 22, 16, 10, 18, 26, 20, 12, 8, 16, 22, 14, 8, 12, 18, 10]

    var body: some View {
        TimelineView(.animation(minimumInterval: 1 / 30, paused: reduceMotion)) { context in
            let t = context.date.timeIntervalSinceReferenceDate
            HStack(spacing: 4) {
                ForEach(0..<28, id: \.self) { i in
                    let lit = i < 18
                    let base = lit ? heights[i % heights.count] : 4
                    let wobble = lit && !reduceMotion ? CGFloat(0.65 + 0.35 * sin(t * 9 + Double(i) * 0.8)) : 1
                    Capsule()
                        .fill(lit ? DNColor.actionAccent : DNColor.neutral400)
                        .frame(width: 3, height: max(4, base * wobble))
                }
            }
            .frame(height: 28)
        }
    }
}
