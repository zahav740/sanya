package com.alexey.sanya_selero

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log

object NetworkUtils {
    fun isInternetAvailable(context: Context): Boolean {
        return try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork
                val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
                networkCapabilities != null && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } else {
                val networkInfo = connectivityManager.activeNetworkInfo
                networkInfo != null && networkInfo.isConnected
            }.also {
                Log.i("NetworkUtils", "Проверка интернет-соединения выполнена: Доступен - $it")
            }
        } catch (e: Exception) {
            Log.e("NetworkUtils", "Ошибка при проверке интернет-соединения: ${e.message}", e)
            false
        }
    }
}
