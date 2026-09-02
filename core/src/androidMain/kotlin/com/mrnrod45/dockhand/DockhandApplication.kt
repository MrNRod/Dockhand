package com.mrnrod45.dockhand

import android.app.Application
import android.content.Context

class DockhandApplication : Application() {
    companion object {
        lateinit var context: Context
            private set
    }

    override fun onCreate() {
        super.onCreate()
        context = this
    }
}
