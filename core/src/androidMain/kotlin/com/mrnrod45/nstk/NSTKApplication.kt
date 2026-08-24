package com.mrnrod45.nstk

import android.app.Application
import android.content.Context

class NSTKApplication : Application() {
    companion object {
        lateinit var context: Context
            private set
    }

    override fun onCreate() {
        super.onCreate()
        context = this
    }
}
