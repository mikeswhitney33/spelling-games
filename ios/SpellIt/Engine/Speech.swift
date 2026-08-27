import AVFoundation

/// Native text-to-speech for "Hear it" and Listen & Spell.
final class Speaker {
    static let shared = Speaker()

    private let synthesizer = AVSpeechSynthesizer()

    private init() {
        try? AVAudioSession.sharedInstance().setCategory(.playback, mode: .spokenAudio)
    }

    func speak(_ text: String, slow: Bool = true) {
        try? AVAudioSession.sharedInstance().setActive(true)
        synthesizer.stopSpeaking(at: .immediate)
        let utterance = AVSpeechUtterance(string: text)
        utterance.voice = AVSpeechSynthesisVoice(language: "en-US")
        utterance.rate = slow ? AVSpeechUtteranceDefaultSpeechRate * 0.8 : AVSpeechUtteranceDefaultSpeechRate
        synthesizer.speak(utterance)
    }
}
