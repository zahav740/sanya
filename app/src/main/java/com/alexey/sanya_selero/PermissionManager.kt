package com.alexey.sanya_selero

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionManager {
    private val requiredPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_NETWORK_STATE
    )

    fun requestPermissions(activity: AppCompatActivity, onComplete: (Boolean) -> Unit) {
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, missingPermissions.toTypedArray(), 1)
            Log.i("PermissionManager", "Запрос разрешений отправлен.")
        } else {
            onComplete(true)
            Log.i("PermissionManager", "Все необходимые разрешения уже предоставлены.")
        }
    }

    fun handlePermissionsResult(requestCode: Int, grantResults: IntArray, onComplete: (Boolean) -> Unit) {
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            onComplete(true)
            Log.i("PermissionManager", "Разрешения успешно получены.")
        } else {
            onComplete(false)
            Log.e("PermissionManager", "Некоторые разрешения не были предоставлены.")
        }
    }
}
