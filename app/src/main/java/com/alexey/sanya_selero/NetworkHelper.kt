package com.alexey.sanya_selero

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import kotlin.concurrent.thread

class NetworkHelper(private val serverUrl: String) {
    private val client = OkHttpClient()

    fun sendJsonToServer(jsonFilePath: File, onResponseReceived: (String) -> Unit) {
        thread {
            try {
                val body = jsonFilePath.asRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(serverUrl)
                    .post(body)
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e("NetworkHelper", "Error sending JSON to server", e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            val responseJson = JSONObject(response.body?.string() ?: "")
                            val responseText = responseJson.getString("response")
                            Log.d("NetworkHelper", "Received response from server: $responseText")
                            onResponseReceived(responseText)
                        } else {
                            Log.e("NetworkHelper", "Server error: ${response.code}")
                        }
                    }
                })
            } catch (e: Exception) {
                Log.e("NetworkHelper", "Error sending JSON to server", e)
            }
        }
    }
}
