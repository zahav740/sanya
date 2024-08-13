package com.alexey.sanya_selero

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class NetworkHelper(private val serverUrl: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun sendJsonToServer(jsonFilePath: File, onResponse: (String) -> Unit, onError: (Exception) -> Unit) {
        thread {
            try {
                val body = jsonFilePath.asRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(serverUrl)
                    .post(body)
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        onError(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        response.use {
                            if (response.isSuccessful) {
                                val responseBody = response.body?.string()
                                onResponse(responseBody ?: "Empty response")
                            } else {
                                onError(IOException("Unexpected response code ${response.code}"))
                            }
                        }
                    }
                })
            } catch (e: Exception) {
                onError(e)
            }
        }
    }
}
