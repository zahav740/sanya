package com.alexey.sanya_selero

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.audiorecorder.R
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.io.IOException
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity(), SRecognitionManager.RecognitionCallback {

    private lateinit var transcriptionTextView: TextView
    private lateinit var recognitionManager: SRecognitionManager
    private lateinit var jsonFilePath: File
    private lateinit var voiceController: VoiceController
    private val serverUrl = "https://your-secure-server.com/processVoice" // Используем HTTPS
    private val networkHelper = NetworkHelper(serverUrl)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        transcriptionTextView = findViewById(R.id.transcriptionTextView)
        recognitionManager = SRecognitionManager(this, this)

        // Используем относительный путь для хранения файла JSON
        jsonFilePath = File(getExternalFilesDir(null), "text.json")
        Log.d("MainActivity", "JSON file path: ${jsonFilePath.absolutePath}")

        // Инициализация VoiceController
        voiceController = VoiceController(this)

        // Настройка параметров голоса
        voiceController.setVoicePitch(1.2f) // Высота голоса
        voiceController.setVoiceSpeed(1.3f) // Скорость речи

        requestPermissions()
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_NETWORK_STATE)
        }
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 1)
        } else {
            recognitionManager.startListeningForTrigger() // Начинаем слушать триггерное слово
        }
    }

    override fun onReadyForSpeech() {
        Log.d("MainActivity", "Ready for speech")
    }

    override fun onBeginningOfSpeech() {
        Log.d("MainActivity", "Beginning of speech")
    }

    override fun onEndOfSpeech() {
        Log.d("MainActivity", "End of speech")
    }

    override fun onError(error: Int) {
        Log.e("MainActivity", "Speech recognition error: $error")
    }

    override fun onNetworkError() {
        Toast.makeText(this, "Проблемы с интернет-соединением", Toast.LENGTH_SHORT).show()
    }

    override fun onResults(results: String) {
        if (results.contains("Саня", ignoreCase = true)) {
            Toast.makeText(this, "Триггер обнаружен!", Toast.LENGTH_SHORT).show()
            recognitionManager.startListeningForSpeech() // Начинаем распознавание речи
        } else {
            transcriptionTextView.text = results
            appendTextToJson(results)
            networkHelper.sendJsonToServer(jsonFilePath, ::handleServerResponse, ::handleServerError)
        }
    }

    override fun onPartialResults(partialResults: String) {
        transcriptionTextView.text = partialResults
        appendTextToJson(partialResults)
    }

    private fun appendTextToJson(text: String) {
        if (text.isNotBlank()) {
            thread {
                try {
                    val json = JSONObject().apply {
                        put("text", text)
                        put("source", "smartphone")
                    }
                    FileWriter(jsonFilePath).use { it.write(json.toString()) }
                    Log.d("JsonHelper", "Saved to JSON: ${json.toString()}")
                } catch (e: IOException) {
                    Log.e("JsonHelper", "Error writing to JSON", e)
                } catch (e: Exception) {
                    Log.e("JsonHelper", "Unexpected error", e)
                }
            }
        } else {
            Log.d("JsonHelper", "Empty text, not saving to JSON")
        }
    }

    private fun handleServerResponse(responseText: String) {
        Log.d("MainActivity", "Received response from server: $responseText")
        runOnUiThread {
            playResponse(responseText)
        }
    }

    private fun handleServerError(e: Exception) {
        Log.e("MainActivity", "Error sending JSON to server", e)
        runOnUiThread {
            Toast.makeText(this, "Ошибка отправки на сервер: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playResponse(responseText: String) {
        voiceController.speak(responseText)
        voiceController.setOnSpeechCompleteListener(object : VoiceController.SpeechCompleteListener {
            override fun onSpeechComplete() {
                runOnUiThread {
                    recognitionManager.startListeningForTrigger() // Возвращаемся к ожиданию триггерного слова
                }
            }

            override fun onError() {
                runOnUiThread {
                    recognitionManager.startListeningForTrigger() // Возвращаемся к ожиданию триггерного слова
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        recognitionManager.destroy()
        voiceController.shutdown()
        Log.i("MainActivity", "MainActivity завершил работу без ошибок.")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            recognitionManager.startListeningForTrigger()
        } else {
            Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show()
        }
    }
}
