//package com.alexey.sanya_selero
//
//import android.content.Context
//import android.media.MediaRecorder
//import android.os.Environment
//import android.os.Handler
//import android.os.Looper
//import android.util.Log
//import java.io.File
//import java.io.IOException
//
//class AudioRecorder(private val context: Context) {
//
//    private var mediaRecorder: MediaRecorder? = null
//    private val maxDuration = 58000 // 58 seconds
//    private val silenceThreshold = 2000 // 2 seconds
//    private var silenceStartTime: Long = 0
//    private val handler = Handler(Looper.getMainLooper())
//
//    fun startRecording() {
//        val musicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
//        val outputFile = File(musicDir, "audio.wav")
//        mediaRecorder = MediaRecorder().apply {
//            setAudioSource(MediaRecorder.AudioSource.MIC)
//            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
//            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
//            setOutputFile(outputFile.absolutePath)
//            setMaxDuration(maxDuration)
//            prepare()
//            start()
//        }
//
//        Log.d("AudioRecorder", "Recording started, saving to: ${outputFile.absolutePath}")
//
//        handler.postDelayed(checkSilenceRunnable, 100)
//    }
//
//    private val checkSilenceRunnable = object : Runnable {
//        override fun run() {
//            mediaRecorder?.let {
//                val maxAmplitude = it.maxAmplitude
//                if (maxAmplitude < 1000) { // Silence threshold
//                    if (silenceStartTime == 0L) {
//                        silenceStartTime = System.currentTimeMillis()
//                    } else if (System.currentTimeMillis() - silenceStartTime > silenceThreshold) {
//                        stopRecording()
//                        return
//                    }
//                } else {
//                    silenceStartTime = 0
//                }
//                handler.postDelayed(this, 100)
//            }
//        }
//    }
//
//    fun stopRecording() {
//        mediaRecorder?.apply {
//            stop()
//            release()
//        }
//        mediaRecorder = null
//        handler.removeCallbacks(checkSilenceRunnable)
//        Log.d("AudioRecorder", "Recording stopped")
//    }
//}