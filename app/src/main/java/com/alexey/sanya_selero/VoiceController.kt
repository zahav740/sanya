package com.alexey.sanya_selero

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class VoiceController(private val context: Context) : TextToSpeech.OnInitListener {

    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false

    init {
        textToSpeech = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale("ru"))

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("VoiceController", "Язык не поддерживается")
            } else {
                isInitialized = true
                setYouthfulVoice()
            }
        } else {
            Log.e("VoiceController", "Инициализация не удалась")
        }
    }

    fun setVoicePitch(pitch: Float) {
        textToSpeech?.setPitch(pitch)
    }

    fun setVoiceSpeed(speed: Float) {
        textToSpeech?.setSpeechRate(speed)
    }

    private fun setYouthfulVoice() {
        val availableVoices = textToSpeech?.voices
        val youthfulVoice = availableVoices?.firstOrNull { voice ->
            voice.name.contains("ru-ru-x-iog-network")
        }

        if (youthfulVoice != null) {
            textToSpeech?.voice = youthfulVoice
            Log.d("VoiceController", "Выбран юношеский голос: ${youthfulVoice.name}")
        } else {
            Log.d("VoiceController", "Юношеский голос не найден, использую стандартный")
        }
    }

    fun speak(text: String) {
        if (isInitialized) {
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "UtteranceId")
        } else {
            Log.e("VoiceController", "TextToSpeech не инициализирован")
        }
    }

    fun setOnSpeechCompleteListener(listener: SpeechCompleteListener) {
        textToSpeech?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                listener.onSpeechComplete()
            }

            override fun onError(utteranceId: String?) {
                listener.onError()
            }
        })
    }

    fun shutdown() {
        textToSpeech?.shutdown()
    }

    interface SpeechCompleteListener {
        fun onSpeechComplete()
        fun onError()
    }
}
