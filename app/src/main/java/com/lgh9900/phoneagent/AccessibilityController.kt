package com.lgh9900.phoneagent

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.delay

class AccessibilityController private constructor(
    private val service: AccessibilityService
) {

    companion object {
        @Volatile
        private var instance: AccessibilityController? = null

        fun initialize(service: AccessibilityService) {
            instance = AccessibilityController(service)
        }

        fun getInstance(): AccessibilityController? = instance
    }


    fun click(x: Int, y: Int): Boolean {
        val path = Path().apply {
            moveTo(x.toFloat(), y.toFloat())
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
            .build()

        return service.dispatchGesture(gesture, null, null)
    }


    fun longPress(x: Int, y: Int): Boolean {
        val path = Path().apply {
            moveTo(x.toFloat(), y.toFloat())
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 1000))
            .build()

        return service.dispatchGesture(gesture, null, null)
    }


    suspend fun doubleTap(x: Int, y: Int): Boolean {
        click(x, y)
        delay(100)
        return click(x, y)
    }


    fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, duration: Long = 300): Boolean {
        val path = Path().apply {
            moveTo(x1.toFloat(), y1.toFloat())
            lineTo(x2.toFloat(), y2.toFloat())
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()

        return service.dispatchGesture(gesture, null, null)
    }


    fun inputText(text: String): Boolean {
        try {
            val clipboard = service.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("input", text)
            clipboard.setPrimaryClip(clip)

            val root = service.rootInActiveWindow ?: return false
            val focusedNode = findFocusedEditText(root)

            if (focusedNode != null) {
                focusedNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                Thread.sleep(100)
                focusedNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)
                return true
            }

            val editTexts = findEditTexts(root)
            if (editTexts.isNotEmpty()) {
                val target = editTexts.first()
                target.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                Thread.sleep(100)
                target.performAction(AccessibilityNodeInfo.ACTION_PASTE)
                return true
            }

            return false
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }


    fun performBack(): Boolean {
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    }


    fun performHome(): Boolean {
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
    }


    fun performRecents(): Boolean {
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
    }


    fun launchApp(packageName: String): Boolean {
        return try {
            val intent = service.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                service.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }


    fun findAndClickText(text: String): Boolean {
        val root = service.rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByText(text)

        return if (nodes.isNotEmpty()) {
            nodes[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
            true
        } else {
            false
        }
    }


    private fun findFocusedEditText(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null

        if (node.isFocused && node.className?.contains("EditText") == true) {
            return node
        }

        for (i in 0 until node.childCount) {
            val result = findFocusedEditText(node.getChild(i))
            if (result != null) return result
        }

        return null
    }


    private fun findEditTexts(node: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> {
        if (node == null) return emptyList()

        val result = mutableListOf<AccessibilityNodeInfo>()

        if (node.className?.contains("EditText") == true) {
            result.add(node)
        }

        for (i in 0 until node.childCount) {
            result.addAll(findEditTexts(node.getChild(i)))
        }

        return result
    }
}