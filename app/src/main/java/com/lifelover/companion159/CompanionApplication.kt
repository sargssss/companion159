package com.lifelover.companion159

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CompanionApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d("CompanionApplication", "🚀 Application started")
    }
}