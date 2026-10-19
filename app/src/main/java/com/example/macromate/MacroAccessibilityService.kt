package com.example.macromate

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

class MacroAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile var instance: MacroAccessibilityService? = null
        @Volatile var recording = false
        @Volatile var onRecorded: ((MacroAction) -> Unit)? = null
    }

    private var lastEventAt = 0L
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!recording || event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_CLICKED &&
            event.eventType != AccessibilityEvent.TYPE_VIEW_LONG_CLICKED) return
        val node = event.source ?: return
        val rect = android.graphics.Rect()
        node.getBoundsInScreen(rect)
        node.recycle()
        if (rect.isEmpty) return
        val now = System.currentTimeMillis()
        val delay = if (lastEventAt == 0L) 0L else (now - lastEventAt).coerceAtMost(60000L)
        lastEventAt = now
        onRecorded?.invoke(MacroAction(rect.centerX(), rect.centerY(), delay,
            if (event.eventType == AccessibilityEvent.TYPE_VIEW_LONG_CLICKED) "Long click" else "Tap"))
    }

    fun tap(x: Int, y: Int, callback: (() -> Unit)? = null) {
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 70))
            .build()
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) { callback?.invoke() }
            override fun onCancelled(gestureDescription: GestureDescription?) { callback?.invoke() }
        }, Handler(Looper.getMainLooper()))
    }

    override fun onInterrupt() {}
    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }
}
