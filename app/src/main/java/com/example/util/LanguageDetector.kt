package com.example.util

import com.example.data.model.TtsLanguage

object LanguageDetector {

    data class TextAnalysis(
        val detectedLanguage: TtsLanguage,
        val charCount: Int,
        val wordCount: Int,
        val estimatedDurationSeconds: Int
    ) {
        val formattedEstimatedDuration: String
            get() {
                if (charCount == 0) return "--"
                val mins = estimatedDurationSeconds / 60
                val secs = estimatedDurationSeconds % 60
                return if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
            }
    }

    fun analyze(text: String, overrideLanguage: TtsLanguage = TtsLanguage.AUTO): TextAnalysis {
        val trimmed = text.trim()
        val charCount = trimmed.length

        var bengaliChars = 0
        var hindiChars = 0
        var latinChars = 0

        for (ch in trimmed) {
            val code = ch.code
            when {
                code in 0x0980..0x09FF -> bengaliChars++
                code in 0x0900..0x097F -> hindiChars++
                (code in 0x0041..0x005A) || (code in 0x0061..0x007A) -> latinChars++
            }
        }

        val autoDetected = when {
            bengaliChars > hindiChars && bengaliChars > latinChars -> TtsLanguage.BENGALI
            hindiChars > bengaliChars && hindiChars > latinChars -> TtsLanguage.HINDI
            latinChars > 0 -> TtsLanguage.ENGLISH
            bengaliChars > 0 -> TtsLanguage.BENGALI
            hindiChars > 0 -> TtsLanguage.HINDI
            else -> TtsLanguage.ENGLISH
        }

        val finalLanguage = if (overrideLanguage == TtsLanguage.AUTO) autoDetected else overrideLanguage

        val words = if (trimmed.isEmpty()) emptyList() else trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
        val wordCount = words.size

        // Average speaking rate: ~140 words per minute, or ~14 characters per second for Indic scripts
        val estimatedSecs = when {
            charCount == 0 -> 0
            wordCount > 0 -> ((wordCount.toDouble() / 140.0) * 60.0).toInt().coerceAtLeast(1)
            else -> (charCount / 14).coerceAtLeast(1)
        }

        return TextAnalysis(
            detectedLanguage = finalLanguage,
            charCount = charCount,
            wordCount = wordCount,
            estimatedDurationSeconds = estimatedSecs
        )
    }
}
