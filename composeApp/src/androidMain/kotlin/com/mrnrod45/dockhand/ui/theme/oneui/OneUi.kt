package com.mrnrod45.dockhand.ui.theme.oneui

import android.content.Context
import android.os.Build

/**
 * Detects Samsung One UI, which is the only thing the theme branches on.
 *
 * There is no public Samsung SDK for this, so the primary signal is the system feature Samsung
 * declares on every One UI build — queried through [android.content.pm.PackageManager.hasSystemFeature],
 * which is public API. The secondary signal exists because the user has no manual override any
 * more: if Samsung ever renames the feature, Samsung hardware carrying a One UI framework class
 * still resolves correctly, and a Galaxy running a custom AOSP ROM still resolves to Material.
 */
object OneUi {

    private const val FEATURE_SAMSUNG_EXPERIENCE = "com.samsung.feature.samsung_experience_mobile"
    private const val FEATURE_SAMSUNG_EXPERIENCE_LITE = "com.samsung.feature.samsung_experience_mobile_lite"

    /** Shipped on One UI, absent from AOSP and other OEM ROMs. */
    private const val CLASS_SEM_FLOATING_FEATURE = "com.samsung.android.feature.SemFloatingFeature"

    private val isSamsungHardware: Boolean by lazy {
        Build.MANUFACTURER.orEmpty().equals("samsung", ignoreCase = true) ||
            Build.BRAND.orEmpty().equals("samsung", ignoreCase = true)
    }

    private val hasOneUiFramework: Boolean by lazy {
        runCatching { Class.forName(CLASS_SEM_FLOATING_FEATURE) }.isSuccess
    }

    fun isOneUi(context: Context): Boolean {
        val pm = context.packageManager
        val declaresFeature = pm.hasSystemFeature(FEATURE_SAMSUNG_EXPERIENCE) ||
            pm.hasSystemFeature(FEATURE_SAMSUNG_EXPERIENCE_LITE)
        return declaresFeature || (isSamsungHardware && hasOneUiFramework)
    }
}
