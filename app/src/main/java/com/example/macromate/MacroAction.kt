package com.example.macromate

data class MacroAction(
    val x: Int,
    val y: Int,
    val delayMs: Long,
    val label: String = "Tap"
)
