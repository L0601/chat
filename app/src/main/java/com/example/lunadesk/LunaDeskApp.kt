package com.example.lunadesk

import android.app.Application
import com.example.lunadesk.data.AppContainer

class LunaDeskApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

