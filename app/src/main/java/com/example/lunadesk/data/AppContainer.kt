package com.example.lunadesk.data

import android.content.Context
import com.example.lunadesk.data.local.SettingsRepository
import com.example.lunadesk.data.remote.LmStudioRepository
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val settingsRepository = SettingsRepository(context)
    val lmStudioRepository = LmStudioRepository(okHttpClient)
}

