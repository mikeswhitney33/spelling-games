package com.skdaddle.spellit.engine

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Native text-to-speech for "Hear it" and Listen & Spell. */
class Speaker private constructor(context: Context) {
    companion object {
        @Volatile
        private var instance: Speaker? = null

        fun shared(context: Context): Speaker =
            instance ?: synchronized(this) {
                instance ?: Speaker(context.applicationContext).also { instance = it }
            }
    }

    private var ready = false
    private var pending: Pair<String, Boolean>? = null

    private lateinit var tts: TextToSpeech

    init {
        // The init callback fires asynchronously, after the property below is
        // assigned, so the self-reference inside it is safe.
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ready = true
                tts.language = Locale.US
                pending?.let { (text, slow) -> speak(text, slow) }
                pending = null
            }
        }
    }

    fun speak(text: String, slow: Boolean = true) {
        if (!ready) {
            // The engine binds asynchronously; keep the latest request so the
            // first tap after launch still talks once the engine arrives.
            pending = text to slow
            return
        }
        tts.setSpeechRate(if (slow) 0.8f else 1.0f)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "spellit-utterance")
    }
}
