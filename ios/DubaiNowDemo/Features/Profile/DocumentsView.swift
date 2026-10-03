import SwiftUI

/// Your documents: every document as a wallet card (2 columns; 1 at accessibility sizes).
struct DocumentsView: View {
    @Environment(DemoStore.self) private var store
    @Environment(\.dynamicTypeSize) private var typeSize

    private var needsAttention: [DocumentItem] { store.state.documents.filter { $0.status != .valid } }
    private var valid: [DocumentItem] { store.state.documents.filter { $0.status == .valid } }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                if store.state.documents.isEmpty {
                    ContentUnavailableView(
                        "No documents yet",
                        systemImage: "person.text.rectangle",
                        description: Text("Your Emirates ID, visa and licences show here when you sign in with UAE PASS.")
                    )
                    .padding(.top, 40)
                } else {
                    Text("From the entities that issue them. They update on their own.")
                        .dnFont(.subheadline)
                        .foregroundStyle(DNColor.textSecondary)
                    if !needsAttention.isEmpty { grid(title: "Needs a look", needsAttention) }
                    if !valid.isEmpty { grid(title: "Valid", valid) }
                }
            }
            .padding(.horizontal, DNSpace.insetPage)
            .padding(.bottom, 40)
        }
        .dnCanvasBackground()
        .profileChrome("Your documents")
    }

    private func grid(title: String, _ docs: [DocumentItem]) -> some View {
        let columns = typeSize.isAccessibilitySize
            ? [GridItem(.flexible())]
            : [GridItem(.flexible(), spacing: 12), GridItem(.flexible())]
        return VStack(alignment: .leading, spacing: 12) {
            Text(title)
                .dnFont(.title3)
                .fontWeight(.bold)
                .foregroundStyle(DNColor.textPrimary)
                .accessibilityAddTraits(.isHeader)
            LazyVGrid(columns: columns, spacing: 12) {
                ForEach(docs) { doc in
                    NavigationLink(value: Route.document(doc.id)) {
                        ProfileDocumentCard(document: doc, width: nil)
                    }
                    .buttonStyle(.dnPressable)
                }
            }
        }
    }
}
