package com.example.lunadesk.ui.components

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

private const val TOAST_DURATION_MILLIS = 3_000L

fun showAppToast(
    context: Context,
    message: String,
    previousToast: Toast? = null
): Toast {
    previousToast?.cancel()
    return Toast.makeText(context, message, Toast.LENGTH_LONG).also { toast ->
        toast.show()
        Handler(Looper.getMainLooper()).postDelayed(
            { toast.cancel() },
            TOAST_DURATION_MILLIS
        )
    }
}
