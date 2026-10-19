package com.example.macromate

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private var panel: LinearLayout? = null
    private var params: WindowManager.LayoutParams? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        showPanel()
    }

    private fun showPanel() {
        if (panel != null) return
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 10, 12, 10)
            setBackgroundColor(0xEE20242B.toInt())
        }
        val header = TextView(this).apply {
            text = "MacroMate  ⋮⋮"
            textSize = 14f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(8, 4, 8, 8)
        }
        layout.addView(header)
        layout.addView(Button(this).apply {
            text = "● Record"
            setOnClickListener {
                MacroAccessibilityService.recording = true
                Toast.makeText(this@OverlayService, "Recording enabled", Toast.LENGTH_SHORT).show()
            }
        })
        layout.addView(Button(this).apply {
            text = "■ Stop record"
            setOnClickListener {
                MacroAccessibilityService.recording = false
                Toast.makeText(this@OverlayService, "Recording stopped", Toast.LENGTH_SHORT).show()
            }
        })
        layout.addView(Button(this).apply {
            text = "▶ Play saved"
            setOnClickListener {
                val i = Intent(this@OverlayService, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra("play_saved_from_overlay", true)
                startActivity(i)
            }
        })
        layout.addView(Button(this).apply {
            text = "× Hide"
            setOnClickListener { stopSelf() }
        })

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20; y = 180
        }
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        header.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = lp.x; startY = lp.y
                    touchX = event.rawX; touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    lp.x = startX + (event.rawX - touchX).toInt()
                    lp.y = startY + (event.rawY - touchY).toInt()
                    wm.updateViewLayout(layout, lp)
                    true
                }
                else -> false
            }
        }
        panel = layout
        params = lp
        wm.addView(layout, lp)
    }

    override fun onDestroy() {
        panel?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        panel = null
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
