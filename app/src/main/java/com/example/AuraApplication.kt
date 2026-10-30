package com.example

import android.app.Application

class AuraApplication : Application() {
    companion object {
        var instance: AuraApplication? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
