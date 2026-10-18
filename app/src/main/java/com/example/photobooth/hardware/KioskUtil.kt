package com.example.photobooth.hardware

import android.app.Activity
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController

object KioskUtil {
    fun startKioskMode(activity: Activity) {
        try {
            activity.startLockTask()
            hideSystemUI(activity)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopKioskMode(activity: Activity) {
        try {
            activity.stopLockTask()
            showSystemUI(activity)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hideSystemUI(activity: Activity) {
        val window = activity.window
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        }
    }

    fun showSystemUI(activity: Activity) {
        val window = activity.window
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.insetsController?.show(WindowInsets.Type.systemBars())
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }
}
