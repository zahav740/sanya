package com.alexey.sanya_selero

import android.util.Log
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.io.IOException
import kotlin.concurrent.thread

class JsonHelper(private val jsonFilePath: File) {

    fun appendTextToJson(text: String) {
        thread {
            try {
                val json = try {
                    // Попытка прочитать существующий JSON
                    JSONObject(jsonFilePath.readText())
                } catch (e: Exception) {
                    // Если файл пуст или некорректный, создаем новый JSONObject
                    JSONObject()
                }
                val currentText = json.optString("text", "")
                json.put("text", "$currentText$text ")
                FileWriter(jsonFilePath).use {
                    it.write(json.toString())
                }
                Log.d("JsonHelper", "Запись в JSON выполнена без ошибок: ${json.toString()}")
            } catch (e: IOException) {
                Log.e("JsonHelper", "Ошибка при записи в JSON: ${e.message}", e)
            } catch (e: Exception) {
                Log.e("JsonHelper", "Неожиданная ошибка: ${e.message}", e)
            }
        }
    }
}
