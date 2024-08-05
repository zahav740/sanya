package com.alexey.sanya_selero

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import kotlin.concurrent.thread

class NetworkHelper(private val serverUrl: String) {

    private val client = OkHttpClient()

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
                        Log.e("NetworkHelper", "Error sending JSON to server", e)
                        onError(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        response.use { // Auto-closes the response body
                            if (response.isSuccessful) {
                                val responseBody = response.body?.string()
                                if (responseBody.isNullOrEmpty()) {
                                    Log.e("NetworkHelper", "Empty response body")
                                    onError(IOException("Empty response body"))
                                    return
                                }
                                try {
                                    val responseJson = JSONObject(responseBody)
                                    val responseText = responseJson.optString("response", "Нет ответа")
                                    Log.d("NetworkHelper", "Received response from server: $responseText")
                                    onResponse(responseText)
                                } catch (e: JSONException) {
                                    Log.e("NetworkHelper", "JSON parsing error", e)
                                    onError(e)
                                }
                            } else {
                                Log.e("NetworkHelper", "Server error: ${response.code}")
                                onError(IOException("Server error: ${response.code}"))
                            }
                        }
                    }
                })
            } catch (e: Exception) {
                Log.e("NetworkHelper", "Error sending JSON to server", e)
                onError(e)
            }
        }
    }
}
