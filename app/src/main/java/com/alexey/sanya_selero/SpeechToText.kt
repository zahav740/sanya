//package com.alexey.sanya_selero
//
//import ai.onnxruntime.OnnxTensor
//import ai.onnxruntime.OrtEnvironment
//import ai.onnxruntime.OrtSession
//import android.content.Context
//import android.os.Environment
//import android.util.Log
//import com.microsoft.onnxruntime.OnnxTensor
//import com.microsoft.onnxruntime.OrtEnvironment
//import com.microsoft.onnxruntime.OrtSession
//import java.io.File
//import java.io.FileInputStream
//import java.nio.ByteBuffer
//import java.nio.ByteOrder
//
//class SpeechToText(private val context: Context) {
//
//    private lateinit var env: OrtEnvironment
//    private lateinit var session: OrtSession
//
//    init {
//        try {
//            env = OrtEnvironment.getEnvironment()
//            val modelFile = File(context.getExternalFilesDir(null), "ru_v3.onnx")
//            session = env.createSession(modelFile.absolutePath)
//        } catch (e: Exception) {
//            Log.e("SpeechToText", "Error initializing ONNX Runtime", e)
//        }
//    }
//
//    fun transcribeAudio(): String? {
//        val audioFile = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "audio.wav")
//        if (!audioFile.exists()) {
//            Log.e("SpeechToText", "Audio file not found: ${audioFile.absolutePath}")
//            return null
//        }
//
//        return try {
//            val audioBytes = FileInputStream(audioFile).use { it.readBytes() }
//            val inputBuffer = ByteBuffer.allocateDirect(audioBytes.size).apply {
//                order(ByteOrder.nativeOrder())
//                put(audioBytes)
//            }
//
//            val inputTensor = OnnxTensor.createTensor(env, inputBuffer)
//            val result = session.run(mapOf("input" to inputTensor))
//
//            val outputTensor = result[0].value as Array<FloatArray>
//            val transcript = processOutput(outputTensor)
//
//            Log.d("SpeechToText", "Transcription: $transcript")
//            transcript
//        } catch (e: Exception) {
//            Log.e("SpeechToText", "Error during transcription", e)
//            null
//        }
//    }
//
//    private fun processOutput(outputArray: Array<FloatArray>): String {
//        // Преобразование выходного массива в текстовую строку
//        // Это зависит от конкретной модели и ее выходного формата
//        // Здесь нужно реализовать логику для преобразования выходных данных модели в текст
//        return outputArray.joinToString(separator = " ") { it.joinToString(separator = " ") }
//    }
//}