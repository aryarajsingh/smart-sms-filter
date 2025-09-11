package com.smartsmsfilter

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SmartSmsFilterApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
    }
}
