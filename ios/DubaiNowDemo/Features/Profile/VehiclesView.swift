import SwiftUI

/// Your vehicles: one card per vehicle (plate, registration, fines) → the vehicle page.
struct VehiclesView: View {
    @Environment(DemoStore.self) private var store

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                if store.state.vehicles.isEmpty {
                    ContentUnavailableView(
                        "No vehicles yet",
                        systemImage: "car",
                        description: Text("Vehicles registered to you with RTA show here on their own.")
                    )
                    .padding(.top, 40)
                } else {
                    Text("Registered to you with RTA. They update on their own.")
                        .dnFont(.subheadline)
                        .foregroundStyle(DNColor.textSecondary)
                    ForEach(store.state.vehicles) { vehicle in
                        NavigationLink(value: Route.vehicle(vehicle.id)) {
                            ProfileVehicleCard(vehicle: vehicle, fines: store.profileFines(for: vehicle))
                        }
                        .buttonStyle(.dnPressable)
                    }
                }

                ProfileGroup {
                    if let route = ProfileRoutes.route(forService: "register-vehicle") {
                        NavigationLink(value: route) {
                            ProfileRow(title: "Register a vehicle", subtitle: "A new or used car you've bought") {
                                ProfileGreyDisc(symbol: "plus")
                            }
                        }
                        .buttonStyle(ProfileRowButtonStyle())
                    }
                    NavigationLink(value: Route.subtopic(topic: "driving-and-transport", sub: "vehicles")) {
                        ProfileRow(title: "All vehicle services", subtitle: "RTA, Dubai Police and Salik") {
                            ProfileGreyDisc(symbol: "square.grid.2x2")
                        }
                    }
                    .buttonStyle(ProfileRowButtonStyle())
                }
                .padding(.top, 8)
            }
            .padding(.horizontal, DNSpace.insetPage)
            .padding(.bottom, 40)
        }
        .dnCanvasBackground()
        .profileChrome("Your vehicles")
    }
}

private struct ProfileVehicleCard: View {
    var vehicle: Vehicle
    var fines: [Payable]

    var body: some View {
        let days = DemoDate.daysFromToday(vehicle.registrationExpiry)
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .top) {
                NumberPlate(code: vehicle.plate.code, number: vehicle.plate.number, emirate: vehicle.plate.emirate)
                Spacer(minLength: 8)
                Image(systemName: "chevron.forward")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(DNColor.listRowChevron)
                    .padding(.top, 10)
                    .accessibilityHidden(true)
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(vehicle.shortName)
                    .dnFont(.title3)
                    .foregroundStyle(DNColor.textPrimary)
                Text("\(String(vehicle.year)) · \(vehicle.colour) · \(vehicle.insurer)")
                    .dnFont(.footnote)
                    .foregroundStyle(DNColor.textSecondary)
            }
            ProfileFlowLayout(spacing: 8, lineSpacing: 8) {
                if days < 0 {
                    StatusChip(text: "Registration expired", tone: .error, onWhite: true)
                } else if days <= 60 {
                    StatusChip(text: "Registration · \(days) days left", tone: .warning, onWhite: true)
                } else {
                    StatusChip(text: "Registered to \(DemoDate.monthYear(vehicle.registrationExpiry))", tone: .success, onWhite: true)
                }
                if !fines.isEmpty {
                    let total = fines.reduce(Decimal(0)) { $0 + $1.amount }
                    StatusChip(text: "\(fines.count == 1 ? "1 fine" : "\(fines.count) fines") · \(Money.aed(total, trimWhole: true))", tone: .error, onWhite: true)
                }
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .dnCard(radius: DNRadius.listGroup)
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isButton)
    }
}
