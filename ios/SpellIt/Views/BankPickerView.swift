import SwiftUI

/// Word-list selector shown in every bank-driven game, with a jump to the
/// list manager.
struct BankPickerView: View {
    @State private var store = BankStore.shared
    @State private var showSettings = false

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Word list")
                .font(.heading(13, weight: .medium))
                .foregroundStyle(Color.mutedInk)
            HStack(spacing: 12) {
                Menu {
                    Section("Levels") {
                        bankButtons(store.allBanks.filter { $0.builtIn && $0.id.hasPrefix("band-") })
                    }
                    Section("Collections") {
                        bankButtons(store.allBanks.filter { $0.builtIn && !$0.id.hasPrefix("band-") })
                    }
                    if store.customBanks.isEmpty == false {
                        Section("My lists") {
                            bankButtons(store.customBanks)
                        }
                    }
                } label: {
                    HStack(spacing: 6) {
                        Text(store.activeBank.name)
                            .font(.heading(15, weight: .medium))
                            .lineLimit(1)
                        Image(systemName: "chevron.up.chevron.down")
                            .font(.system(size: 11, weight: .bold))
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 9)
                    .background(
                        ZStack {
                            RoundedRectangle(cornerRadius: 12).fill(Color.ink).offset(y: 3)
                            RoundedRectangle(cornerRadius: 12)
                                .fill(Color.white)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 12)
                                        .strokeBorder(Color.ink, lineWidth: 2.5)
                                )
                        }
                    )
                    .foregroundStyle(Color.ink)
                }
                Button {
                    showSettings = true
                } label: {
                    Label("Manage lists", systemImage: "gearshape.fill")
                        .font(.heading(13, weight: .medium))
                        .foregroundStyle(Color.mutedInk)
                }
            }
            Text(store.activeBank.blurb)
                .font(.system(size: 13))
                .foregroundStyle(Color.mutedInk)
        }
        .sheet(isPresented: $showSettings) {
            SettingsView()
        }
    }

    private func bankButtons(_ banks: [WordBank]) -> some View {
        ForEach(banks) { bank in
            Button {
                store.setActive(bank.id)
            } label: {
                if bank.id == store.activeId {
                    Label(bank.name, systemImage: "checkmark")
                } else {
                    Text(bank.name)
                }
            }
        }
    }
}

/// Shown when the active bank lacks enough usable words for a game.
struct NotEnoughWordsView: View {
    var need: Int
    var requirement: String

    @State private var showSettings = false

    var body: some View {
        VStack(spacing: 10) {
            Text("This list needs more words for this game.")
                .font(.heading(17, weight: .semibold))
                .foregroundStyle(Color.ink)
                .multilineTextAlignment(.center)
            Text("It takes at least \(need) \(requirement) to play. Add some, or pick a different list above.")
                .font(.system(size: 14))
                .foregroundStyle(Color.mutedInk)
                .multilineTextAlignment(.center)
            Button("Manage word lists") {
                showSettings = true
            }
            .buttonStyle(ChunkyButtonStyle(bordered: true))
        }
        .padding(.vertical, 12)
        .sheet(isPresented: $showSettings) {
            SettingsView()
        }
    }
}
