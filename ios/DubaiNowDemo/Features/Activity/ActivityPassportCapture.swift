import SwiftUI

// MARK: - Presentation (scan or upload → advance the request → "Sent to GDRFA")

extension View {
    /// Attaches the fake passport scan (full screen, camera-like) and the "Upload file" picker.
    /// Both finish the same way: the request moves on and a success toast says where it went.
    func activityPassportCapture(scan: Binding<ActivityCaptureTarget?>, upload: Binding<ActivityCaptureTarget?>) -> some View {
        modifier(ActivityCaptureModifier(scan: scan, upload: upload))
    }
}

private struct ActivityCaptureModifier: ViewModifier {
    @Binding var scan: ActivityCaptureTarget?
    @Binding var upload: ActivityCaptureTarget?
    @State private var sentRequestID: String?
    @Environment(DemoStore.self) private var store
    @Environment(ToastCenter.self) private var toasts

    func body(content: Content) -> some View {
        content
            .fullScreenCover(item: $scan, onDismiss: finishScan) { target in
                if let request = store.request(target.id) {
                    ActivityPassportCaptureView(request: request) { sentRequestID = target.id }
                }
            }
            .confirmationDialog(
                "Upload your passport copy",
                isPresented: Binding(get: { upload != nil }, set: { if !$0 { upload = nil } }),
                titleVisibility: .visible,
                presenting: upload
            ) { target in
                Button("Passport scan.pdf · Files") { send(target.id, after: .milliseconds(350)) }
                Button("Passport photo page · Photos") { send(target.id, after: .milliseconds(350)) }
                Button("Cancel", role: .cancel) {}
            } message: { _ in
                Text("Choose the photo page of your passport. It needs to be sharp, with all four corners showing.")
            }
    }

    private func finishScan() {
        guard let id = sentRequestID else { return }
        sentRequestID = nil
        send(id, after: .milliseconds(150))
    }

    private func send(_ id: String, after delay: Duration) {
        let entity = store.request(id)?.entity ?? "government"
        Task { @MainActor in
            try? await Task.sleep(for: delay)
            store.advanceRequest(id)
            DNHaptics.success()
            toasts.show("Sent to \(entity)")
        }
    }
}

// MARK: - The capture screen

/// A camera-like dark screen with a passport frame guide and a shutter. Nothing is captured:
/// the "feed" is a drawn passport page that comes into focus, like a real scanner would.
struct ActivityPassportCaptureView: View {
    let request: RequestItem
    var onUse: () -> Void

    @Environment(\.dismiss) private var dismiss
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var isSharp = false
    @State private var captured = false
    @State private var flash = false

    var body: some View {
        ZStack {
            LinearGradient(colors: [DNColor.ink900, DNColor.ink950], startPoint: .top, endPoint: .bottom)
                .ignoresSafeArea()

            VStack(spacing: 0) {
                topBar
                Spacer(minLength: 12)
                viewfinder
                Spacer(minLength: 12)
                controls
            }

            DNColor.white
                .opacity(flash ? 0.85 : 0)
                .ignoresSafeArea()
                .allowsHitTesting(false)
        }
        .task {
            // The "camera" settles and focuses, then says it's ready.
            try? await Task.sleep(for: .milliseconds(reduceMotion ? 100 : 900))
            withAnimation(reduceMotion ? DNMotion.base : .easeOut(duration: 0.5)) { isSharp = true }
            if ActivityDemoArg.value == "shoot" || ActivityDemoArg.value == "send" {
                try? await Task.sleep(for: .milliseconds(900))
                shoot()
            }
            if ActivityDemoArg.value == "send" {
                try? await Task.sleep(for: .milliseconds(1200))
                onUse()
                dismiss()
            }
        }
    }

    // MARK: Top

    private var topBar: some View {
        HStack {
            GlassIconButton(symbol: "xmark", label: "Close") { dismiss() }
                .environment(\.colorScheme, .dark)
            Spacer()
            VStack(spacing: 2) {
                Text("Passport photo page")
                    .dnFont(.headline)
                    .foregroundStyle(DNColor.textOnInverse)
                Text(request.title)
                    .dnFont(.footnote)
                    .foregroundStyle(DNColor.widgetOnInverseSecondary)
            }
            Spacer()
            Color.clear.frame(width: 44, height: 44)
        }
        .padding(.horizontal, DNSpace.insetPage)
        .padding(.top, 8)
    }

    // MARK: Viewfinder

    private var viewfinder: some View {
        VStack(spacing: 18) {
            ZStack {
                ActivityPassportPage()
                    .blur(radius: isSharp || captured ? 0 : 5)
                    .scaleEffect(isSharp || captured ? 1 : 1.04)
                    .padding(18)
                ActivityFrameCorners()
                    .stroke(isSharp ? DNColor.green300 : DNColor.white, style: StrokeStyle(lineWidth: 4, lineCap: .round, lineJoin: .round))
            }
            .aspectRatio(1.42, contentMode: .fit)
            .padding(.horizontal, 24)
            .accessibilityElement()
            .accessibilityLabel(captured ? "Photo of your passport page" : "Camera viewfinder with a frame for your passport page")

            HStack(spacing: 8) {
                Image(systemName: isSharp || captured ? "checkmark.circle.fill" : "viewfinder")
                    .foregroundStyle(isSharp || captured ? DNColor.green300 : DNColor.white)
                Text(statusText)
                    .dnFont(.subheadline)
                    .fontWeight(.medium)
                    .foregroundStyle(DNColor.textOnInverse)
            }
            .padding(.horizontal, 14)
            .frame(minHeight: 36)
            .background(DNColor.white.opacity(0.12), in: .capsule)
            .contentTransition(.opacity)
            .accessibilityElement(children: .combine)
        }
    }

    private var statusText: String {
        if captured { return "Sharp, and all four corners show" }
        return isSharp ? "Looks sharp · take the photo" : "Hold steady inside the frame"
    }

    // MARK: Controls

    @ViewBuilder private var controls: some View {
        if captured {
            VStack(spacing: 10) {
                Button("Use this photo") {
                    onUse()
                    dismiss()
                }
                .buttonStyle(.dn(.primary))
                Button("Retake") {
                    withAnimation(DNMotion.resolve(DNMotion.snappy)) { captured = false }
                }
                .dnFont(.body)
                .fontWeight(.semibold)
                .foregroundStyle(DNColor.textOnInverse)
                .frame(maxWidth: .infinity, minHeight: 48)
                .contentShape(.rect)
                .buttonStyle(.plain)
            }
            .padding(.horizontal, DNSpace.insetPage)
            .padding(.bottom, 12)
            .transition(.opacity)
        } else {
            Button(action: shoot) {
                ZStack {
                    Circle().strokeBorder(DNColor.white, lineWidth: 4).frame(width: 78, height: 78)
                    Circle().fill(DNColor.white).frame(width: 64, height: 64)
                }
                .frame(width: 88, height: 88)
                .contentShape(.circle)
            }
            .buttonStyle(.dnPressable)
            .accessibilityLabel("Take photo")
            .padding(.bottom, 24)
            .transition(.opacity)
        }
    }

    private func shoot() {
        DNHaptics.light()
        if !isSharp { isSharp = true }
        if reduceMotion {
            captured = true
            return
        }
        withAnimation(.easeOut(duration: 0.08)) { flash = true }
        Task { @MainActor in
            try? await Task.sleep(for: .milliseconds(90))
            withAnimation(.easeIn(duration: 0.3)) { flash = false }
            withAnimation(DNMotion.snappy) { captured = true }
        }
    }
}

/// Four rounded corner brackets (the frame guide).
private struct ActivityFrameCorners: Shape {
    func path(in rect: CGRect) -> Path {
        let l = min(rect.width, rect.height) * 0.16
        let r: CGFloat = 14
        var p = Path()
        // top left
        p.move(to: CGPoint(x: rect.minX, y: rect.minY + l))
        p.addLine(to: CGPoint(x: rect.minX, y: rect.minY + r))
        p.addQuadCurve(to: CGPoint(x: rect.minX + r, y: rect.minY), control: CGPoint(x: rect.minX, y: rect.minY))
        p.addLine(to: CGPoint(x: rect.minX + l, y: rect.minY))
        // top right
        p.move(to: CGPoint(x: rect.maxX - l, y: rect.minY))
        p.addLine(to: CGPoint(x: rect.maxX - r, y: rect.minY))
        p.addQuadCurve(to: CGPoint(x: rect.maxX, y: rect.minY + r), control: CGPoint(x: rect.maxX, y: rect.minY))
        p.addLine(to: CGPoint(x: rect.maxX, y: rect.minY + l))
        // bottom right
        p.move(to: CGPoint(x: rect.maxX, y: rect.maxY - l))
        p.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY - r))
        p.addQuadCurve(to: CGPoint(x: rect.maxX - r, y: rect.maxY), control: CGPoint(x: rect.maxX, y: rect.maxY))
        p.addLine(to: CGPoint(x: rect.maxX - l, y: rect.maxY))
        // bottom left
        p.move(to: CGPoint(x: rect.minX + l, y: rect.maxY))
        p.addLine(to: CGPoint(x: rect.minX + r, y: rect.maxY))
        p.addQuadCurve(to: CGPoint(x: rect.minX, y: rect.maxY - r), control: CGPoint(x: rect.minX, y: rect.maxY))
        p.addLine(to: CGPoint(x: rect.minX, y: rect.maxY - l))
        return p
    }
}

/// A drawn passport photo page (no real data: the persona's name and placeholder lines).
private struct ActivityPassportPage: View {
    var body: some View {
        RoundedRectangle(cornerRadius: 10, style: .continuous)
            .fill(LinearGradient(colors: [DNColor.huePeachStart, DNColor.hueLilacStart], startPoint: .topLeading, endPoint: .bottomTrailing))
            .overlay(alignment: .topLeading) {
                GeometryReader { geo in
                    let w = geo.size.width, h = geo.size.height
                    HStack(alignment: .top, spacing: w * 0.05) {
                        RoundedRectangle(cornerRadius: 6, style: .continuous)
                            .fill(DNColor.hueLilacEnd)
                            .overlay(
                                Image(systemName: "person.fill")
                                    .resizable()
                                    .scaledToFit()
                                    .padding(w * 0.04)
                                    .foregroundStyle(DNColor.hueLilacInk.opacity(0.55))
                            )
                            .frame(width: w * 0.28, height: h * 0.56)
                        VStack(alignment: .leading, spacing: h * 0.045) {
                            ForEach([0.7, 0.9, 0.55, 0.8, 0.6, 0.75], id: \.self) { f in
                                Capsule().fill(DNColor.hueLilacInk.opacity(0.22)).frame(width: w * 0.55 * f, height: h * 0.035)
                            }
                        }
                        .padding(.top, h * 0.03)
                    }
                    .padding(.leading, w * 0.05)
                    .padding(.top, h * 0.08)

                    VStack(alignment: .leading, spacing: 2) {
                        Text("P<LBNHADDAD<<SARA<<<<<<<<<<<<<<<<<<<<<")
                        Text("RL<<<<821<5LBN9003147F3106307<<<<<<<<<")
                    }
                    .font(.system(size: max(7, w * 0.032), weight: .medium, design: .monospaced))
                    .foregroundStyle(DNColor.hueLilacInk.opacity(0.7))
                    .lineLimit(1)
                    .minimumScaleFactor(0.5)
                    .padding(.horizontal, w * 0.05)
                    .padding(.bottom, h * 0.07)
                    .frame(width: w, height: h, alignment: .bottomLeading)
                }
            }
            .shadow(color: DNColor.ink950.opacity(0.5), radius: 18, x: 0, y: 10)
            .accessibilityHidden(true)
    }
}
