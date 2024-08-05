package com.alexey.sanya_selero

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.audiorecorder.R
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity(), SRecognitionManager.RecognitionCallback {

    private lateinit var transcriptionTextView: TextView
    private lateinit var recognitionManager: SRecognitionManager
    private lateinit var jsonFilePath: File // Используем lateinit для отложенной инициализации
    private lateinit var tts: TextToSpeech
    private val serverUrl = "http://localhost:3000/processVoice"
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        transcriptionTextView = findViewById(R.id.transcriptionTextView)
        recognitionManager = SRecognitionManager(this, this)

        // Инициализация пути к файлу в onCreate()
        jsonFilePath = File(getExternalFilesDir(null), "text.json")
        Log.d("MainActivity", "JSON file path: ${jsonFilePath.absolutePath}")

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale("ru")
            } else {
                Log.e("MainActivity", "TTS Initialization failed")
            }
        }

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
        }
    }

    override fun onPartialResults(partialResults: String) {
        transcriptionTextView.text = partialResults
        appendTextToJson(partialResults)
    }

    private fun appendTextToJson(text: String) {
        thread {
            try {
                val json = JSONObject().apply {
                    put("text", text)
                }
                FileWriter(jsonFilePath).use { it.write(json.toString()) }
                Log.d("JsonHelper", "Saved to JSON: ${json.toString()}")
            } catch (e: IOException) {
                Log.e("JsonHelper", "Error writing to JSON", e)
            } catch (e: Exception) {
                Log.e("JsonHelper", "Unexpected error", e)
            }
        }
    }

    private fun sendJsonToServer() {
        thread {
            try {
                val json = jsonFilePath.readText()
                Log.d("MainActivity", "Sending JSON to server: $json")
                val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(serverUrl)
                    .post(body)
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e("MainActivity", "Error sending JSON to server", e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            val responseJson = JSONObject(response.body?.string() ?: "")
                            val responseText = responseJson.getString("response")
                            Log.d("MainActivity", "Received response from server: $responseText")
                            runOnUiThread {
                                playResponse(responseText)
                            }
                        } else {
                            Log.e("MainActivity", "Server error: ${response.code}")
                        }
                    }
                })
            } catch (e: Exception) {
                Log.e("MainActivity", "Error sending JSON to server", e)
            }
        }
    }

    private fun playResponse(responseText: String) {
        tts.speak(responseText, TextToSpeech.QUEUE_FLUSH, null, null)
        tts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                runOnUiThread {
                    recognitionManager.startListeningForTrigger() // Возвращаемся к ожиданию триггерного слова
                }
            }

            override fun onError(utteranceId: String?) {
                runOnUiThread {
                    recognitionManager.startListeningForTrigger() // Возвращаемся к ожиданию триггерного слова
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        recognitionManager.destroy()
        tts.shutdown()
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