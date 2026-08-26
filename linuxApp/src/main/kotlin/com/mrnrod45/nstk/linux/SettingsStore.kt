package com.mrnrod45.nstk.linux

import java.util.prefs.Preferences

/** Tiny wrapper around java.util.prefs — no extra dependency needed for simple flag persistence. */
object SettingsStore {
    private val prefs: Preferences = Preferences.userRoot().node("com/mrnrod45/nstk")

    fun getBool(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)
    fun setBool(key: String, value: Boolean) = prefs.putBoolean(key, value)
}
