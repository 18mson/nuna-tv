package com.nunatv.app.util

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration

object DeviceUtils {

    /**
     * Memeriksa apakah aplikasi berjalan pada perangkat Android TV.
     * Menggabungkan pengecekan UiModeManager dan PackageManager Leanback feature.
     */
    fun isTvDevice(context: Context): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        val isUiModeTv = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION

        val hasLeanbackFeature = context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        val hasNoTouchscreen = !context.packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)

        return isUiModeTv || hasLeanbackFeature || hasNoTouchscreen
    }
}
