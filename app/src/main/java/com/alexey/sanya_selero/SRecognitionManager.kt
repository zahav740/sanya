package com.alexey.sanya_selero

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale
import java.util.Timer
import java.util.TimerTask

class SRecognitionManager(private val context: Context, private val listener: RecognitionCallback) {

    private val speechRecognizer: SpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    private var isListening: Boolean = false
    private val silenceTimeout: Long = 2000
    private var silenceTimer: Timer? = null
    private val handler = Handler(Looper.getMainLooper())

    init {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("SpeechRecognition", "Ready for speech")
                listener.onReadyForSpeech()
            }

            override fun onBeginningOfSpeech() {
                Log.d("SpeechRecognition", "Beginning of speech")
                listener.onBeginningOfSpeech()
                resetSilenceTimer()
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d("SpeechRecognition", "End of speech")
                listener.onEndOfSpeech()
                stopListening()
                listener.onResults("") // Отправляем пустую строку, чтобы запустить отправку JSON на сервер
            }

            override fun onError(error: Int) {
                Log.e("SpeechRecognition", "Speech recognition error: $error")
                listener.onError(error)
                when (error) {
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        startListeningForTrigger()
                    }
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                        handler.postDelayed({ startListeningForTrigger() }, 1000)
                    }
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                        listener.onNetworkError()
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.let {
                    val fullText = it.joinToString(" ")
                    listener.onResults(fullText)
                    resetSilenceTimer()
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                partial?.let {
                    listener.onPartialResults(it.joinToString(" "))
                    resetSilenceTimer()
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    fun startListeningForTrigger() {
        if (!isListening) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("ru"))
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            isListening = true
            speechRecognizer.startListening(intent)
            Log.i("SRecognitionManager", "Начато прослушивание триггерного слова без ошибок.")
        }
    }

    fun startListeningForSpeech() {
        if (!isListening) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("ru"))
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, silenceTimeout)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, silenceTimeout)
            }
            isListening = true
            speechRecognizer.startListening(intent)
            Log.i("SRecognitionManager", "Начато распознавание речи без ошибок.")
        }
    }

    fun stopListening() {
        if (isListening) {
            isListening = false
            speechRecognizer.stopListening()
            silenceTimer?.cancel()
            Log.i("SRecognitionManager", "Прослушивание остановлено без ошибок.")
        }
    }

    fun destroy() {
        speechRecognizer.destroy()
        Log.i("SRecognitionManager", "Ресурсы освобождены без ошибок.")
    }

    private fun resetSilenceTimer() {
        silenceTimer?.cancel()
        silenceTimer = Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    handler.post {
                        stopListening()
                        listener.onResults("")
                    }
                }
            }, silenceTimeout)
        }
    }

    interface RecognitionCallback {
        fun onReadyForSpeech()
        fun onBeginningOfSpeech()
        fun onEndOfSpeech()
        fun onError(error: Int)
        fun onResults(results: String)
        fun onPartialResults(partialResults: String)
        fun onNetworkError()
    }
}
