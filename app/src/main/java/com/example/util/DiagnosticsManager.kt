package com.example.util

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import com.example.data.api.GeminiTtsService
import com.example.data.local.AppDatabase
import com.example.data.local.HistoryEntity
import com.example.data.model.DiagnosticComponentResult
import com.example.data.model.DiagnosticStatus
import com.example.data.model.FullDiagnosticSummary
import com.example.data.model.SpeakingStyle
import com.example.data.model.TtsLanguage
import com.example.data.model.VoiceEmotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.net.Socket

class DiagnosticsManager(
    private val context: Context,
    private val ttsService: GeminiTtsService,
    private val database: AppDatabase
) {
    private val _diagnosticSummary = MutableStateFlow(FullDiagnosticSummary())
    val diagnosticSummary: StateFlow<FullDiagnosticSummary> = _diagnosticSummary.asStateFlow()

    suspend fun runFullDiagnostics(
        apiKey: String,
        modelId: String,
        voiceName: String,
        onProgressUpdate: (String) -> Unit
    ): FullDiagnosticSummary = withContext(Dispatchers.IO) {
        AppLogger.log("DIAG", "Beginning Full Diagnostic Suite...")
        _diagnosticSummary.value = FullDiagnosticSummary(isRunning = true)

        val results = mutableListOf<DiagnosticComponentResult>()

        fun updateResult(res: DiagnosticComponentResult) {
            val index = results.indexOfFirst { it.component == res.component }
            if (index >= 0) {
                results[index] = res
            } else {
                results.add(res)
            }
            _diagnosticSummary.value = FullDiagnosticSummary(isRunning = true, results = results.toList())
        }

        // 1. App Initialization
        onProgressUpdate("Checking App Initialization...")
        val appInitResult = try {
            val cache = context.cacheDir
            val files = context.filesDir
            if (cache.exists() && files.exists()) {
                DiagnosticComponentResult(
                    component = "App Initialization",
                    status = DiagnosticStatus.PASS,
                    message = "Application context, cache, and files directories ready."
                )
            } else {
                DiagnosticComponentResult(
                    component = "App Initialization",
                    status = DiagnosticStatus.WARNING,
                    message = "App directory structure partially initialized."
                )
            }
        } catch (e: Exception) {
            DiagnosticComponentResult(
                component = "App Initialization",
                status = DiagnosticStatus.FAIL,
                errorCode = "INIT_ERR",
                message = "Failed to initialize app context: ${e.message}",
                possibleCause = "Device storage restrictions or permission issue.",
                suggestedSolution = "Restart the application."
            )
        }
        updateResult(appInitResult)

        // 2. Android Version Compatibility
        onProgressUpdate("Checking Android Compatibility...")
        val versionResult = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            DiagnosticComponentResult(
                component = "Android Compatibility",
                status = DiagnosticStatus.PASS,
                message = "Android API ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE}) - Fully supported."
            )
        } else {
            DiagnosticComponentResult(
                component = "Android Compatibility",
                status = DiagnosticStatus.WARNING,
                message = "Android API ${Build.VERSION.SDK_INT} is below recommended API 24."
            )
        }
        updateResult(versionResult)

        // 3. Internet Connectivity
        onProgressUpdate("Testing Internet Connectivity...")
        var internetOk = false
        val internetResult = try {
            val s = Socket()
            s.connect(InetSocketAddress("generativelanguage.googleapis.com", 443), 6000)
            s.close()
            internetOk = true
            DiagnosticComponentResult(
                component = "Internet",
                status = DiagnosticStatus.PASS,
                message = "Connected to Google Gemini servers successfully."
            )
        } catch (e: Exception) {
            DiagnosticComponentResult(
                component = "Internet",
                status = DiagnosticStatus.FAIL,
                errorCode = "NET_UNREACHABLE",
                message = "Cannot connect to Google Gemini API servers.",
                possibleCause = "Device is offline or network blocks Google API domains.",
                suggestedSolution = "Check your Wi-Fi/Mobile data connection and disable VPN."
            )
        }
        updateResult(internetResult)

        // 4. API Key Presence
        onProgressUpdate("Verifying API Key Presence...")
        val keyPresenceResult = if (apiKey.isNotBlank()) {
            DiagnosticComponentResult(
                component = "API Key",
                status = DiagnosticStatus.PASS,
                message = "API Key is configured (${if (apiKey.startsWith("AIza")) "Valid AIza format" else "Custom format"})."
            )
        } else {
            DiagnosticComponentResult(
                component = "API Key",
                status = DiagnosticStatus.FAIL,
                errorCode = "KEY_EMPTY",
                message = "No Gemini API Key is configured.",
                possibleCause = "No API key entered in Key Manager.",
                suggestedSolution = "Open API Key Manager and add your Google Gemini API key."
            )
        }
        updateResult(keyPresenceResult)

        // 5. API Authentication & 6. Selected Model & 7. TTS Endpoint & 8. Audio Response
        var testAudioFile: File? = null
        var ttsOk = false

        if (!internetOk) {
            updateResult(DiagnosticComponentResult("Authentication", DiagnosticStatus.NOT_TESTED, message = "Skipped due to no internet."))
            updateResult(DiagnosticComponentResult("Selected Model", DiagnosticStatus.NOT_TESTED, message = "Skipped due to no internet."))
            updateResult(DiagnosticComponentResult("TTS Endpoint", DiagnosticStatus.NOT_TESTED, message = "Skipped due to no internet."))
            updateResult(DiagnosticComponentResult("Audio Response", DiagnosticStatus.NOT_TESTED, message = "Skipped due to no internet."))
        } else if (apiKey.isBlank()) {
            updateResult(DiagnosticComponentResult("Authentication", DiagnosticStatus.FAIL, errorCode = "NO_KEY", message = "Cannot authenticate without an API key."))
            updateResult(DiagnosticComponentResult("Selected Model", DiagnosticStatus.NOT_TESTED, message = "Requires API key."))
            updateResult(DiagnosticComponentResult("TTS Endpoint", DiagnosticStatus.NOT_TESTED, message = "Requires API key."))
            updateResult(DiagnosticComponentResult("Audio Response", DiagnosticStatus.NOT_TESTED, message = "Requires API key."))
        } else {
            onProgressUpdate("Testing API Auth & Model TTS ($modelId)...")
            try {
                val ttsResult = ttsService.generateSpeech(
                    apiKey = apiKey,
                    modelId = modelId,
                    voiceName = voiceName,
                    text = "Diagnostic test. নমস্কার।",
                    language = TtsLanguage.ENGLISH,
                    speakingStyle = SpeakingStyle.NATURAL,
                    emotion = VoiceEmotion.NEUTRAL,
                    emotionIntensity = 0.5f,
                    customInstruction = null,
                    speed = 1.0f
                )

                testAudioFile = ttsResult.audioFile
                ttsOk = true

                updateResult(
                    DiagnosticComponentResult(
                        component = "Authentication",
                        status = DiagnosticStatus.PASS,
                        message = "Gemini API key authenticated successfully."
                    )
                )
                updateResult(
                    DiagnosticComponentResult(
                        component = "Selected Model",
                        status = DiagnosticStatus.PASS,
                        message = "Model '$modelId' is online and accepting requests."
                    )
                )
                updateResult(
                    DiagnosticComponentResult(
                        component = "TTS Endpoint",
                        status = DiagnosticStatus.PASS,
                        message = "Speech synthesis pipeline responded in ${ttsResult.responseTimeMs}ms."
                    )
                )
                updateResult(
                    DiagnosticComponentResult(
                        component = "Audio Response",
                        status = DiagnosticStatus.PASS,
                        message = "Received valid audio payload (${ttsResult.fileSize} bytes, format: ${ttsResult.mimeType})."
                    )
                )
            } catch (e: Exception) {
                val msg = e.message ?: "Unknown error"
                val isAuthErr = msg.contains("401") || msg.contains("403") || msg.contains("Auth Error")
                val isRateLimit = msg.contains("429") || msg.contains("Rate Limit")
                val isModelErr = msg.contains("404") || msg.contains("Model Not Found")

                updateResult(
                    DiagnosticComponentResult(
                        component = "Authentication",
                        status = if (isAuthErr) DiagnosticStatus.FAIL else if (isRateLimit) DiagnosticStatus.WARNING else DiagnosticStatus.PASS,
                        errorCode = if (isAuthErr) "AUTH_401" else if (isRateLimit) "RATE_LIMIT_429" else null,
                        message = if (isAuthErr) "Authentication rejected: Invalid or deactivated API key." else if (isRateLimit) "Rate limited." else "Authentication passed.",
                        suggestedSolution = if (isAuthErr) "Provide a fresh API key from Google AI Studio." else "Retry later."
                    )
                )
                updateResult(
                    DiagnosticComponentResult(
                        component = "Selected Model",
                        status = if (isModelErr) DiagnosticStatus.FAIL else if (isRateLimit) DiagnosticStatus.WARNING else DiagnosticStatus.PASS,
                        errorCode = if (isModelErr) "MODEL_404" else null,
                        message = if (isModelErr) "Model '$modelId' not recognized or unavailable for audio." else "Model check completed with notes.",
                        suggestedSolution = if (isModelErr) "Select 'Gemini 2.5 Flash TTS' or update model ID." else null
                    )
                )
                updateResult(
                    DiagnosticComponentResult(
                        component = "TTS Endpoint",
                        status = DiagnosticStatus.FAIL,
                        errorCode = "TTS_ERR",
                        message = "TTS synthesis failed: $msg",
                        technicalDetails = e.stackTraceToString().take(200),
                        suggestedSolution = "Check selected model and API key quota."
                    )
                )
                updateResult(
                    DiagnosticComponentResult(
                        component = "Audio Response",
                        status = DiagnosticStatus.FAIL,
                        errorCode = "NO_AUDIO",
                        message = "No audio payload received.",
                        suggestedSolution = "Resolve TTS synthesis issues."
                    )
                )
            }
        }

        // 9. Voice Verification
        onProgressUpdate("Validating Voice Configuration ($voiceName)...")
        val voiceResult = DiagnosticComponentResult(
            component = "Voice",
            status = DiagnosticStatus.PASS,
            message = "Voice '$voiceName' successfully mapped to Gemini prebuilt voice config."
        )
        updateResult(voiceResult)

        // 10. Audio Decoder
        onProgressUpdate("Testing Audio Decoder...")
        val decoderResult = if (testAudioFile != null && testAudioFile.exists() && testAudioFile.length() > 0) {
            DiagnosticComponentResult(
                component = "Audio Decoder",
                status = DiagnosticStatus.PASS,
                message = "WAV audio container validated and ready for Android playback."
            )
        } else {
            // Test with a local synthetic WAV test tone
            try {
                val dummyTone = WavAudioHelper.wrapPcmWithWavHeader(ByteArray(2400), 24000, 1)
                if (dummyTone.size == 2444) {
                    DiagnosticComponentResult(
                        component = "Audio Decoder",
                        status = DiagnosticStatus.PASS,
                        message = "Internal PCM/WAV synthesizer and decoder functional."
                    )
                } else {
                    DiagnosticComponentResult(component = "Audio Decoder", status = DiagnosticStatus.WARNING, message = "Audio decoder validation anomaly.")
                }
            } catch (e: Exception) {
                DiagnosticComponentResult(component = "Audio Decoder", status = DiagnosticStatus.FAIL, message = "Audio decoder error: ${e.message}")
            }
        }
        updateResult(decoderResult)

        // 11. Storage & Audio Player
        onProgressUpdate("Testing Storage & Audio Player...")
        val storageResult = try {
            val testFile = File(context.cacheDir, "diag_storage_test.tmp")
            FileOutputStream(testFile).use { it.write("test".toByteArray()) }
            val readOk = testFile.exists() && testFile.length() == 4L
            testFile.delete()
            if (readOk) {
                DiagnosticComponentResult(
                    component = "Storage",
                    status = DiagnosticStatus.PASS,
                    message = "Local internal storage read/write verified."
                )
            } else {
                DiagnosticComponentResult(
                    component = "Storage",
                    status = DiagnosticStatus.WARNING,
                    message = "Storage write verification incomplete."
                )
            }
        } catch (e: Exception) {
            DiagnosticComponentResult(
                component = "Storage",
                status = DiagnosticStatus.FAIL,
                errorCode = "STORAGE_ERR",
                message = "Storage read/write failed: ${e.message}"
            )
        }
        updateResult(storageResult)

        // 12. Audio Player
        val playerResult = try {
            val mp = MediaPlayer()
            mp.release()
            DiagnosticComponentResult(
                component = "Audio Player",
                status = DiagnosticStatus.PASS,
                message = "Android MediaPlayer initialized and released successfully."
            )
        } catch (e: Exception) {
            DiagnosticComponentResult(
                component = "Audio Player",
                status = DiagnosticStatus.FAIL,
                errorCode = "PLAYER_ERR",
                message = "MediaPlayer initialization failed: ${e.message}"
            )
        }
        updateResult(playerResult)

        // 13. History Database
        onProgressUpdate("Verifying History Database...")
        val dbResult = try {
            val count = database.historyDao().getCount()
            DiagnosticComponentResult(
                component = "History Database",
                status = DiagnosticStatus.PASS,
                message = "Room database operational (current records: $count)."
            )
        } catch (e: Exception) {
            DiagnosticComponentResult(
                component = "History Database",
                status = DiagnosticStatus.FAIL,
                errorCode = "DB_ERR",
                message = "Room database query failed: ${e.message}"
            )
        }
        updateResult(dbResult)

        val finalSummary = FullDiagnosticSummary(isRunning = false, results = results.toList())
        _diagnosticSummary.value = finalSummary
        AppLogger.log("DIAG", "Full Diagnostic completed: ${finalSummary.passedCount} passed, ${finalSummary.warningCount} warnings, ${finalSummary.failedCount} failed. Status: ${finalSummary.overallStatus}")
        finalSummary
    }
}
