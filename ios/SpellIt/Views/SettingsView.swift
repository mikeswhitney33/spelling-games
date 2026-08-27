import SwiftUI

/// Word-list manager: pick the active list, build custom lists, duplicate
/// built-ins as starting points.
struct SettingsView: View {
    @State private var store = BankStore.shared
    @State private var editingBankId: String?
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                Section {
                    Text("Every game draws from one list at a time. A hint powers the clue games; a sentence (using the word once) powers Fix the Sentence.")
                        .font(.system(size: 13))
                        .foregroundStyle(Color.mutedInk)
                        .listRowBackground(Color.clear)
                }

                Section("My lists") {
                    ForEach(store.customBanks) { bank in
                        bankRow(bank)
                    }
                    Button {
                        let bank = store.createBank()
                        editingBankId = bank.id
                    } label: {
                        Label("New list", systemImage: "plus")
                            .font(.heading(15, weight: .medium))
                    }
                }

                Section("Built-in lists") {
                    ForEach(WordData.builtInBanks) { bank in
                        bankRow(bank)
                    }
                }
            }
            .navigationTitle("Word lists")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                }
            }
            .navigationDestination(item: $editingBankId) { id in
                BankEditorView(bankId: id)
            }
        }
        .tint(Color.ink)
    }

    @ViewBuilder
    private func bankRow(_ bank: WordBank) -> some View {
        HStack(spacing: 10) {
            VStack(alignment: .leading, spacing: 2) {
                Text(bank.displayName)
                    .font(.heading(15, weight: .semibold))
                Text("\(bank.entries.count) words\(bank.blurb.isEmpty ? "" : " · \(bank.blurb)")")
                    .font(.system(size: 12))
                    .foregroundStyle(Color.mutedInk)
            }
            Spacer()
            if bank.id == store.activeId {
                Label("In use", systemImage: "checkmark.circle.fill")
                    .font(.heading(12, weight: .medium))
                    .foregroundStyle(Color.leaf)
                    .labelStyle(.titleAndIcon)
            } else {
                Button("Use") {
                    store.setActive(bank.id)
                }
                .buttonStyle(.bordered)
                .font(.heading(13, weight: .medium))
            }
        }
        .swipeActions(edge: .trailing, allowsFullSwipe: false) {
            if !bank.builtIn {
                Button(role: .destructive) {
                    store.deleteBank(bank.id)
                } label: {
                    Label("Delete", systemImage: "trash")
                }
                Button {
                    editingBankId = bank.id
                } label: {
                    Label("Edit", systemImage: "pencil")
                }
            } else {
                Button {
                    let copy = store.createBank(
                        name: "\(bank.name) (my copy)",
                        entries: bank.entries,
                    )
                    editingBankId = copy.id
                } label: {
                    Label("Duplicate", systemImage: "doc.on.doc")
                }
            }
        }
        .contentShape(Rectangle())
        .onTapGesture {
            if !bank.builtIn { editingBankId = bank.id }
        }
    }
}

struct BankEditorView: View {
    let bankId: String
    @State private var store = BankStore.shared

    @State private var word = ""
    @State private var hint = ""
    @State private var sentence = ""
    @State private var error: String?

    private var bank: WordBank? {
        store.customBanks.first { $0.id == bankId }
    }

    var body: some View {
        List {
            if let bank {
                Section("List name") {
                    TextField(
                        "List name",
                        text: Binding(
                            get: { bank.name },
                            set: { store.rename(bankId, to: $0) },
                        ),
                    )
                    .font(.heading(16, weight: .semibold))
                }

                Section("Add a word") {
                    TextField("Word (required)", text: $word)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                    TextField("Hint (optional)", text: $hint)
                    TextField("Sentence (optional, uses the word once)", text: $sentence)
                    if let error {
                        Text(error)
                            .font(.heading(13, weight: .medium))
                            .foregroundStyle(Color.coral)
                    }
                    Button {
                        addWord(to: bank)
                    } label: {
                        Label("Add word", systemImage: "plus")
                            .font(.heading(15, weight: .medium))
                    }
                    .disabled(word.trimmingCharacters(in: .whitespaces).isEmpty)
                }

                if bank.entries.count < 6 {
                    Section {
                        Text("Tip: games work best with at least 6–10 words. Hints unlock Mini Crossword and Memory Match; sentences unlock Fix the Sentence.")
                            .font(.system(size: 12))
                            .foregroundStyle(Color.mutedInk)
                    }
                }

                Section("Words (\(bank.entries.count))") {
                    ForEach(Array(bank.entries.enumerated()), id: \.offset) { index, entry in
                        VStack(alignment: .leading, spacing: 2) {
                            Text(entry.word)
                                .font(.heading(15, weight: .semibold))
                            if let hint = entry.hint {
                                Text(hint)
                                    .font(.system(size: 12))
                                    .foregroundStyle(Color.mutedInk)
                            }
                            if let sentence = entry.sentence {
                                Text(sentence)
                                    .font(.system(size: 12))
                                    .italic()
                                    .foregroundStyle(Color.mutedInk)
                            }
                        }
                        .swipeActions {
                            Button(role: .destructive) {
                                store.removeWord(at: index, from: bankId)
                            } label: {
                                Label("Remove", systemImage: "trash")
                            }
                        }
                    }
                }
            }
        }
        .navigationTitle(bank?.displayName ?? "List")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func addWord(to bank: WordBank) {
        let trimmed = word.trimmingCharacters(in: .whitespaces)
        if trimmed.range(of: "^[A-Za-z]{2,20}$", options: .regularExpression) == nil {
            error = "Words are 2–20 letters, no spaces or symbols."
            return
        }
        if WordData.blockedWords.contains(trimmed.lowercased()) {
            error = "Let's pick a different word."
            return
        }
        if bank.entries.contains(where: { $0.word.lowercased() == trimmed.lowercased() }) {
            error = "That word is already in this list."
            return
        }
        let trimmedSentence = sentence.trimmingCharacters(in: .whitespaces)
        if !trimmedSentence.isEmpty,
           !sentenceUsesWordOnce(word: trimmed, sentence: trimmedSentence) {
            error = "The sentence needs to use the word exactly once, on its own."
            return
        }
        let trimmedHint = hint.trimmingCharacters(in: .whitespaces)
        store.addWord(
            WordEntry(
                word: trimmed.lowercased(),
                hint: trimmedHint.isEmpty ? nil : trimmedHint,
                sentence: trimmedSentence.isEmpty ? nil : trimmedSentence,
            ),
            to: bankId,
        )
        word = ""
        hint = ""
        sentence = ""
        error = nil
    }

    /// Tokenizes exactly like Fix the Sentence, so "well-known" never counts
    /// as "well".
    private func sentenceUsesWordOnce(word: String, sentence: String) -> Bool {
        let isWordChar: (Character) -> Bool = { $0.isLetter || $0 == "'" }
        let cores = sentence.split(separator: " ").map { raw -> String in
            let text = String(raw)
            let prefix = String(text.prefix { !isWordChar($0) })
            let suffix = String(text.reversed().prefix { !isWordChar($0) }.reversed())
            return String(text.dropFirst(prefix.count).dropLast(suffix.count))
        }
        return cores.filter { $0.lowercased() == word.lowercased() }.count == 1
    }
}
