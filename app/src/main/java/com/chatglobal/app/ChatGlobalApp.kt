package com.chatglobal.app

import android.app.Application
import android.util.Log

class ChatGlobalApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
                Log.e("ChatGlobalApp", "Excepción no capturada", throwable)
            }
        }
    }
}
