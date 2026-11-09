package com.example.util

object TextChunker {
    const val DEFAULT_MAX_CHUNK_CHARS = 800

    fun shouldChunk(text: String, maxLimit: Int = DEFAULT_MAX_CHUNK_CHARS): Boolean {
        return text.trim().length > maxLimit
    }

    fun splitIntoChunks(text: String, maxLimit: Int = DEFAULT_MAX_CHUNK_CHARS): List<String> {
        val trimmed = text.trim()
        if (trimmed.length <= maxLimit) return listOf(trimmed)

        val chunks = mutableListOf<String>()
        val paragraphs = trimmed.split(Regex("\n+"))

        val currentChunk = StringBuilder()

        for (para in paragraphs) {
            val p = para.trim()
            if (p.isEmpty()) continue

            if (currentChunk.length + p.length + 1 <= maxLimit) {
                if (currentChunk.isNotEmpty()) currentChunk.append("\n\n")
                currentChunk.append(p)
            } else {
                // Paragraph itself might be longer than maxLimit, break down by sentences
                val sentences = p.split(Regex("(?<=[.!?|।])\\s+"))
                for (sentence in sentences) {
                    val s = sentence.trim()
                    if (s.isEmpty()) continue

                    if (currentChunk.length + s.length + 1 <= maxLimit) {
                        if (currentChunk.isNotEmpty()) currentChunk.append(" ")
                        currentChunk.append(s)
                    } else {
                        if (currentChunk.isNotEmpty()) {
                            chunks.add(currentChunk.toString().trim())
                            currentChunk.clear()
                        }
                        if (s.length <= maxLimit) {
                            currentChunk.append(s)
                        } else {
                            // Break down long sentence by commas or words
                            val words = s.split(Regex("\\s+"))
                            for (word in words) {
                                if (currentChunk.length + word.length + 1 <= maxLimit) {
                                    if (currentChunk.isNotEmpty()) currentChunk.append(" ")
                                    currentChunk.append(word)
                                } else {
                                    if (currentChunk.isNotEmpty()) {
                                        chunks.add(currentChunk.toString().trim())
                                        currentChunk.clear()
                                    }
                                    currentChunk.append(word)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString().trim())
        }

        return chunks.filter { it.isNotBlank() }
    }
}
