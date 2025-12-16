package com.lgh9900.phoneagent

import android.content.Context
import android.graphics.Bitmap
import com.lgh9900.phoneagent.service.ScreenCaptureService

class ScreenshotProvider(private val context: Context) {

    fun captureScreen(): Bitmap? {
        val service = ScreenCaptureService.getInstance()
        return if (service != null) {
            try {
                service.captureScreen()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }
}