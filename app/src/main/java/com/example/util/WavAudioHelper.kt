package com.example.util

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object WavAudioHelper {

    /**
     * Checks if the byte array already contains a WAV (RIFF) or MP3/OGG header.
     * If raw PCM, synthesizes a 44-byte standard PCM RIFF header.
     */
    fun saveAudioBytesToFile(
        rawBytes: ByteArray,
        mimeType: String,
        outputFile: File,
        defaultSampleRate: Int = 24000,
        channels: Int = 1
    ): File {
        if (outputFile.parentFile?.exists() != true) {
            outputFile.parentFile?.mkdirs()
        }

        // Check for RIFF header (WAV)
        val isAlreadyWav = rawBytes.size >= 12 &&
                rawBytes[0] == 'R'.code.toByte() &&
                rawBytes[1] == 'I'.code.toByte() &&
                rawBytes[2] == 'F'.code.toByte() &&
                rawBytes[3] == 'F'.code.toByte() &&
                rawBytes[8] == 'W'.code.toByte() &&
                rawBytes[9] == 'A'.code.toByte() &&
                rawBytes[10] == 'V'.code.toByte() &&
                rawBytes[11] == 'E'.code.toByte()

        // Check for MP3 / OGG header
        val isAlreadyMp3OrOgg = (rawBytes.size >= 3 && rawBytes[0] == 'I'.code.toByte() && rawBytes[1] == 'D'.code.toByte() && rawBytes[2] == '3'.code.toByte()) ||
                (rawBytes.size >= 4 && rawBytes[0] == 'O'.code.toByte() && rawBytes[1] == 'g'.code.toByte() && rawBytes[2] == 'g'.code.toByte() && rawBytes[3] == 'S'.code.toByte()) ||
                (rawBytes.size >= 2 && (rawBytes[0].toInt() and 0xFF) == 0xFF && (rawBytes[1].toInt() and 0xE0) == 0xE0)

        val finalBytes = if (isAlreadyWav || isAlreadyMp3OrOgg) {
            rawBytes
        } else {
            // Parse sample rate from mimeType if available (e.g., "audio/pcm;rate=24000")
            val sampleRate = extractSampleRate(mimeType, defaultSampleRate)
            wrapPcmWithWavHeader(rawBytes, sampleRate, channels)
        }

        FileOutputStream(outputFile).use { fos ->
            fos.write(finalBytes)
            fos.flush()
        }

        return outputFile
    }

    private fun extractSampleRate(mimeType: String, fallbackRate: Int): Int {
        val rateRegex = Regex("rate=(\\d+)")
        val match = rateRegex.find(mimeType)
        return match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: fallbackRate
    }

    /**
     * Constructs a 44-byte standard RIFF WAV header for 16-bit PCM.
     */
    fun wrapPcmWithWavHeader(pcmData: ByteArray, sampleRate: Int, channels: Int): ByteArray {
        val totalAudioLen = pcmData.size
        val totalDataLen = totalAudioLen + 36
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8

        val header = ByteArray(44)
        val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF chunk descriptor
        buffer.put('R'.code.toByte())
        buffer.put('I'.code.toByte())
        buffer.put('F'.code.toByte())
        buffer.put('F'.code.toByte())
        buffer.putInt(totalDataLen)
        buffer.put('W'.code.toByte())
        buffer.put('A'.code.toByte())
        buffer.put('V'.code.toByte())
        buffer.put('E'.code.toByte())

        // "fmt " sub-chunk
        buffer.put('f'.code.toByte())
        buffer.put('m'.code.toByte())
        buffer.put('t'.code.toByte())
        buffer.put(' '.code.toByte())
        buffer.putInt(16) // SubChunk1Size (16 for PCM)
        buffer.putShort(1.toShort()) // AudioFormat (1 for PCM)
        buffer.putShort(channels.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(byteRate)
        buffer.putShort(blockAlign.toShort())
        buffer.putShort(bitsPerSample.toShort())

        // "data" sub-chunk
        buffer.put('d'.code.toByte())
        buffer.put('a'.code.toByte())
        buffer.put('t'.code.toByte())
        buffer.put('a'.code.toByte())
        buffer.putInt(totalAudioLen)

        val out = ByteArrayOutputStream(header.size + pcmData.size)
        out.write(header)
        out.write(pcmData)
        return out.toByteArray()
    }

    /**
     * Joins multiple WAV files into one.
     * Assumes all files have the same sample rate, channels, and bit depth.
     */
    fun joinWavFiles(files: List<File>, outputFile: File): File {
        if (files.isEmpty()) return outputFile
        if (files.size == 1) {
            files[0].copyTo(outputFile, overwrite = true)
            return outputFile
        }

        if (outputFile.parentFile?.exists() != true) {
            outputFile.parentFile?.mkdirs()
        }

        val validFiles = files.filter { it.exists() && it.length() > 44 }
        if (validFiles.isEmpty()) return outputFile

        FileOutputStream(outputFile).use { fos ->
            var totalAudioDataSize = 0L
            var sampleRate = 24000
            var channels = 1
            var bitsPerSample = 16

            // First pass: Read header info from first file and calculate total data size
            validFiles.forEachIndexed { index, file ->
                file.inputStream().use { fis ->
                    val header = ByteArray(44)
                    fis.read(header)
                    
                    if (index == 0) {
                        val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
                        channels = buffer.getShort(22).toInt()
                        sampleRate = buffer.getInt(24)
                        bitsPerSample = buffer.getShort(34).toInt()
                    }
                    
                    totalAudioDataSize += (file.length() - 44)
                }
            }

            // Write new 44-byte header
            val newHeader = wrapPcmWithWavHeader(ByteArray(0), sampleRate, channels)
            // Fix sizes in the synthesized header
            val headerBuffer = ByteBuffer.wrap(newHeader).order(ByteOrder.LITTLE_ENDIAN)
            headerBuffer.putInt(4, (totalAudioDataSize + 36).toInt()) // RIFF size
            headerBuffer.putInt(40, totalAudioDataSize.toInt()) // data size
            
            fos.write(newHeader)

            // Second pass: Write all data chunks
            validFiles.forEach { file ->
                file.inputStream().use { fis ->
                    fis.skip(44)
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (fis.read(buffer).also { read = it } != -1) {
                        fos.write(buffer, 0, read)
                    }
                }
            }
            fos.flush()
        }

        return outputFile
    }
}
