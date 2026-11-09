package com.example.util

import com.example.data.model.TtsLanguage

object SmartTextProcessor {

    data class ProcessedTextResult(
        val originalText: String,
        val normalizedText: String,
        val detectedLanguage: TtsLanguage,
        val charCount: Int,
        val wordCount: Int,
        val sentenceCount: Int,
        val paragraphCount: Int,
        val dialogueCount: Int,
        val estimatedSeconds: Int,
        val chunks: List<String>
    ) {
        val formattedEstimatedDuration: String
            get() {
                if (charCount == 0) return "--"
                val mins = estimatedSeconds / 60
                val secs = estimatedSeconds % 60
                return if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
            }
    }

    fun process(text: String, overrideLanguage: TtsLanguage = TtsLanguage.AUTO): ProcessedTextResult {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return ProcessedTextResult(
                originalText = "",
                normalizedText = "",
                detectedLanguage = if (overrideLanguage != TtsLanguage.AUTO) overrideLanguage else TtsLanguage.BENGALI,
                charCount = 0,
                wordCount = 0,
                sentenceCount = 0,
                paragraphCount = 0,
                dialogueCount = 0,
                estimatedSeconds = 0,
                chunks = emptyList()
            )
        }

        // 1. Language Detection
        val detectedLang = if (overrideLanguage != TtsLanguage.AUTO) {
            overrideLanguage
        } else {
            detectLanguage(trimmed)
        }

        // 2. Normalization & Cleanup (preserve original meaning, never delete important content)
        val normalized = normalizeText(trimmed, detectedLang)

        // 3. Count Paragraphs
        val paragraphs = normalized.split(Regex("\n+")).filter { it.isNotBlank() }
        val paragraphCount = paragraphs.size

        // 4. Count Sentences (Bengali dari '।', periods '.', question '?', exclamation '!')
        val sentenceRegex = Regex("[.!?|।]+")
        val sentences = normalized.split(sentenceRegex).filter { it.isNotBlank() }
        val sentenceCount = if (sentences.isEmpty()) 1 else sentences.size

        // 5. Dialogue Detection (quotes: "", '', “”, ‘’)
        val dialogueRegex = Regex("[\"“«][^\"”»]+[\"”»]|'[^']+'|‘[^’]+’")
        val dialogueMatches = dialogueRegex.findAll(normalized)
        val dialogueCount = dialogueMatches.count()

        // 6. Word & Char Counts
        val charCount = normalized.length
        val words = normalized.split(Regex("\\s+")).filter { it.isNotBlank() }
        val wordCount = words.size

        // 7. Estimated Duration (Bengali/Hindi speech is typically 125-140 words per minute)
        val estimatedSeconds = when (detectedLang) {
            TtsLanguage.BENGALI -> ((wordCount / 130.0) * 60).toInt().coerceAtLeast(1)
            TtsLanguage.HINDI -> ((wordCount / 130.0) * 60).toInt().coerceAtLeast(1)
            TtsLanguage.ENGLISH -> ((wordCount / 150.0) * 60).toInt().coerceAtLeast(1)
            else -> ((wordCount / 135.0) * 60).toInt().coerceAtLeast(1)
        }

        // 8. Chunks division
        val chunks = TextChunker.splitIntoChunks(normalized)

        return ProcessedTextResult(
            originalText = text,
            normalizedText = normalized,
            detectedLanguage = detectedLang,
            charCount = charCount,
            wordCount = wordCount,
            sentenceCount = sentenceCount,
            paragraphCount = paragraphCount,
            dialogueCount = dialogueCount,
            estimatedSeconds = estimatedSeconds,
            chunks = chunks
        )
    }

    private fun detectLanguage(text: String): TtsLanguage {
        var bengaliChars = 0
        var hindiChars = 0
        var latinChars = 0

        for (ch in text) {
            val code = ch.code
            when {
                code in 0x0980..0x09FF -> bengaliChars++
                code in 0x0900..0x097F -> hindiChars++
                (code in 0x0041..0x005A) || (code in 0x0061..0x007A) -> latinChars++
            }
        }

        return when {
            bengaliChars > hindiChars && bengaliChars > latinChars -> TtsLanguage.BENGALI
            hindiChars > bengaliChars && hindiChars > latinChars -> TtsLanguage.HINDI
            latinChars > 0 -> TtsLanguage.ENGLISH
            else -> TtsLanguage.BENGALI
        }
    }

    private fun normalizeText(text: String, language: TtsLanguage): String {
        var result = text

        // Standardize irregular spacing
        result = result.replace(Regex("[ \\t]+"), " ")

        // Ensure punctuation has natural breathing pause space
        result = result.replace(Regex("([।!?])([^ \\n\"”’])"), "$1 $2")
        result = result.replace(Regex("([,;:])([^ \\n0-9\"”’])"), "$1 $2")

        // Handle common abbreviations based on language
        when (language) {
            TtsLanguage.BENGALI -> {
                result = result.replace(Regex("\\bড\\.\\s*"), "ডাক্তার ")
                result = result.replace(Regex("\\bমি\\.\\s*"), "মিস্টার ")
                result = result.replace(Regex("\\bনং\\s*"), "নম্বর ")
                result = result.replace(Regex("\\bপৃঃ\\s*"), "পৃষ্ঠা ")
                result = result.replace(Regex("\\bউঃ\\s*"), "উত্তর: ")
                result = result.replace(Regex("\\bপ্রঃ\\s*"), "প্রশ্ন: ")
            }
            TtsLanguage.HINDI -> {
                result = result.replace(Regex("\\bडा\\.\\s*"), "डॉक्टर ")
                result = result.replace(Regex("\\bपं\\.\\s*"), "पंडित ")
                result = result.replace(Regex("\\bक्र\\.\\s*"), "क्रमांक ")
                result = result.replace(Regex("\\bउ\\.\\s*"), "उत्तर: ")
                result = result.replace(Regex("\\bप्र\\.\\s*"), "प्रश्न: ")
            }
            TtsLanguage.ENGLISH -> {
                result = result.replace(Regex("\\bDr\\.\\s+"), "Doctor ")
                result = result.replace(Regex("\\bMr\\.\\s+"), "Mister ")
                result = result.replace(Regex("\\bMrs\\.\\s+"), "Misses ")
                result = result.replace(Regex("\\bMs\\.\\s+"), "Mizz ")
                result = result.replace(Regex("\\bProf\\.\\s+"), "Professor ")
                result = result.replace(Regex("\\betc\\.\\b"), "etcetera")
                result = result.replace(Regex("\\be\\.g\\.\\b"), "for example")
                result = result.replace(Regex("\\bi\\.e\\.\\b"), "that is")
            }
            else -> {}
        }

        return result.trim()
    }
}
