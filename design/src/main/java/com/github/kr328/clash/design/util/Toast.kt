package com.github.kr328.clash.design.util

import android.content.Context
import com.github.kr328.clash.design.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun Context.showExceptionToast(message: CharSequence) {
    withContext(Dispatchers.Main) {
        MaterialAlertDialogBuilder(this@showExceptionToast)
            .setTitle(R.string.error)
            .setMessage(message)
            .setCancelable(true)
            .setPositiveButton(R.string.ok) { _, _ -> }
            .show()
    }
}

suspend fun Context.showExceptionToast(exception: Exception) {
    showExceptionToast(exception.message ?: "Unknown")
}
