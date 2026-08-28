import SwiftUI

@main
struct SpellItApp: App {
    var body: some Scene {
        WindowGroup {
            // The palette is a fixed light one (cream paper, ink text), so the
            // app opts out of dark mode rather than inheriting white system
            // text that vanishes on its light surfaces.
            HomeView()
                .preferredColorScheme(.light)
        }
    }
}
