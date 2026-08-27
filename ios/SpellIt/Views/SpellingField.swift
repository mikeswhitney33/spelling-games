import SwiftUI

/// The chunky text field kids type spellings into, shared across games.
struct SpellingField: View {
    var placeholder: String
    @Binding var text: String
    var onSubmit: () -> Void
    var focused: FocusState<Bool>.Binding

    var body: some View {
        TextField(placeholder, text: $text)
            .font(.heading(22, weight: .semibold))
            .multilineTextAlignment(.center)
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled()
            .keyboardType(.asciiCapable)
            .submitLabel(.done)
            .focused(focused)
            .onSubmit(onSubmit)
            .padding(.vertical, 12)
            .padding(.horizontal, 14)
            .background(
                ZStack {
                    RoundedRectangle(cornerRadius: 14)
                        .fill(Color.ink)
                        .offset(y: 4)
                    RoundedRectangle(cornerRadius: 14)
                        .fill(Color.white)
                        .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(Color.ink, lineWidth: 3))
                }
            )
            .foregroundStyle(Color.ink)
    }
}
