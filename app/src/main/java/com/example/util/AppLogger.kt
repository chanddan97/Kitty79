package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

object AppLogger {
    private const val MAX_LOGS = 500
    private val logList = CopyOnWriteArrayList<String>()
    private val _logsFlow = MutableStateFlow<List<String>>(emptyList())
    val logsFlow: StateFlow<List<String>> = _logsFlow.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun log(tag: String, message: String) {
        val safeMessage = maskSensitiveInfo(message)
        val time = timeFormat.format(Date())
        val entry = "[$time] [$tag] $safeMessage"

        Log.d("KRITY_TTS", entry)

        logList.add(0, entry)
        if (logList.size > MAX_LOGS) {
            logList.removeAt(logList.size - 1)
        }
        _logsFlow.value = logList.toList()
    }

    private fun maskSensitiveInfo(text: String): String {
        // Mask any Google API key pattern (AIzaSy...)
        return text.replace(Regex("AIza[0-9A-Za-z-_]{35}")) { match ->
            val v = match.value
            "${v.take(4)}••••••••${v.takeLast(4)}"
        }
    }

    fun clear() {
        logList.clear()
        _logsFlow.value = emptyList()
    }

    fun getAllLogsFormatted(): String {
        return logList.reversed().joinToString("\n")
    }

    fun copyToClipboard(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("KRITY TTS Debug Log", getAllLogsFormatted())
        clipboard.setPrimaryClip(clip)
    }
}
