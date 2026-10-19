package com.example.macromate

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.net.Uri
import android.os.Build
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {
    private val actions = mutableListOf<MacroAction>()
    private lateinit var listText: TextView
    private lateinit var status: TextView
    private lateinit var xInput: EditText
    private lateinit var yInput: EditText
    private lateinit var delayInput: EditText
    private lateinit var repeatInput: EditText
    private var playing = false
    private var runId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        root.addView(TextView(this).apply { text = "MacroMate"; textSize = 26f })
        status = TextView(this).apply { text = "Accessibility service: check below"; textSize = 14f }
        root.addView(status)
        root.addView(button("Open accessibility settings") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        root.addView(button("Enable floating overlay") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                getSharedPreferences("overlay", MODE_PRIVATE).edit().putBoolean("start_after_permission", true).apply()
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            } else {
                startService(Intent(this, OverlayService::class.java))
            }
        })
        root.addView(TextView(this).apply {
            text = "Recording captures accessible button clicks in other apps. Some games use custom surfaces and may not expose clicks; use manual coordinates there."
        })
        root.addView(button("Start recording clicks") {
            MacroAccessibilityService.onRecorded = { action ->
                runOnUiThread {
                    val previous = actions.lastOrNull()
                    actions.add(action.copy(delayMs = if (previous == null) 0L else action.delayMs))
                    refreshList()
                }
            }
            MacroAccessibilityService.recording = true
            status.text = "Recording accessible clicks…"
        })
        root.addView(button("Stop recording") {
            MacroAccessibilityService.recording = false
            status.text = "Recording stopped"
        })

        val coordRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        xInput = field("X coordinate")
        yInput = field("Y coordinate")
        delayInput = field("Delay ms", "500")
        coordRow.addView(xInput, weightParams())
        coordRow.addView(yInput, weightParams())
        root.addView(coordRow)
        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(delayInput, weightParams())
            addView(button("Add tap") { addManualTap() }, weightParams())
        })
        root.addView(TextView(this).apply { text = "Repeat count (1–100)" })
        repeatInput = field("Repeat count", "1")
        root.addView(repeatInput)
        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(button("▶ Play") { playMacro() }, weightParams())
            addView(button("■ Stop") { stopMacro() }, weightParams())
        })
        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(button("Save macro") { saveMacro() }, weightParams())
            addView(button("Load macro") { loadMacro() }, weightParams())
            addView(button("Clear") { stopMacro(); actions.clear(); refreshList() }, weightParams())
        })
        listText = TextView(this).apply { textSize = 14f }
        val scroll = ScrollView(this)
        scroll.addView(listText)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        refreshList()
    }

    override fun onResume() {
        super.onResume()
        if (getSharedPreferences("overlay", MODE_PRIVATE).getBoolean("start_after_permission", false)) {
            getSharedPreferences("overlay", MODE_PRIVATE).edit().putBoolean("start_after_permission", false).apply()
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
                startService(Intent(this, OverlayService::class.java))
            }
        }
    }

    private fun field(hintText: String, value: String = "") = EditText(this).apply {
        hint = hintText; setSingleLine(true); setText(value)
        inputType = android.text.InputType.TYPE_CLASS_NUMBER
    }
    private fun weightParams() = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    private fun button(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        setOnClickListener { action() }
    }

    private fun addManualTap() {
        val x = xInput.text.toString().toIntOrNull()
        val y = yInput.text.toString().toIntOrNull()
        val delay = delayInput.text.toString().toLongOrNull()
        if (x == null || y == null || delay == null || x < 0 || y < 0 || delay < 0) {
            Toast.makeText(this, "Enter valid X, Y and delay values", Toast.LENGTH_SHORT).show(); return
        }
        actions.add(MacroAction(x, y, delay))
        refreshList()
    }

    private fun refreshList() {
        if (!::listText.isInitialized) return
        listText.text = if (actions.isEmpty()) "No actions yet." else actions.mapIndexed { i, a ->
            "${i + 1}. ${a.label} at (${a.x}, ${a.y}) — wait ${a.delayMs} ms"
        }.joinToString("\n")
    }

    private fun playMacro() {
        val service = MacroAccessibilityService.instance
        if (service == null) {
            Toast.makeText(this, "Enable MacroMate in Accessibility settings first", Toast.LENGTH_LONG).show(); return
        }
        if (actions.isEmpty()) { Toast.makeText(this, "Add or record actions first", Toast.LENGTH_SHORT).show(); return }
        val repeats = (repeatInput.text.toString().toIntOrNull() ?: 1).coerceIn(1, 100)
        playing = true
        val myRun = ++runId
        var index = 0
        var pass = 0
        fun next() {
            if (!playing || myRun != runId) return
            if (index >= actions.size) {
                pass++; index = 0
                if (pass >= repeats) { playing = false; status.text = "Playback finished"; return }
            }
            val action = actions[index++]
            android.os.Handler(mainLooper).postDelayed({
                if (!playing || myRun != runId) return@postDelayed
                service.tap(action.x, action.y) { runOnUiThread { next() } }
            }, action.delayMs.coerceAtMost(60000L))
        }
        status.text = "Playing macro…"
        next()
    }

    private fun stopMacro() {
        playing = false; runId++; status.text = "Stopped"
    }

    private fun saveMacro() {
        val arr = JSONArray()
        actions.forEach { a -> arr.put(JSONObject().put("x", a.x).put("y", a.y).put("delay", a.delayMs).put("label", a.label)) }
        getSharedPreferences("macros", MODE_PRIVATE).edit().putString("last", arr.toString()).apply()
        Toast.makeText(this, "Macro saved on this device", Toast.LENGTH_SHORT).show()
    }

    private fun loadMacro() {
        val raw = getSharedPreferences("macros", MODE_PRIVATE).getString("last", null)
        if (raw == null) { Toast.makeText(this, "No saved macro found", Toast.LENGTH_SHORT).show(); return }
        try {
            val arr = JSONArray(raw)
            actions.clear()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                actions.add(MacroAction(o.getInt("x"), o.getInt("y"), o.getLong("delay"), o.optString("label", "Tap")))
            }
            refreshList()
        } catch (_: Exception) { Toast.makeText(this, "Could not load macro", Toast.LENGTH_SHORT).show() }
    }
}
