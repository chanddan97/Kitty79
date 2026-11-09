package com.example.data.api

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Base64
import com.example.data.model.SpeakingStyle
import com.example.data.model.TtsLanguage
import com.example.data.model.VoiceEmotion
import com.example.util.AppLogger
import com.example.util.WavAudioHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class GeminiTtsService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    data class TtsGenerationResult(
        val audioFile: File,
        val mimeType: String,
        val fileSize: Long,
        val durationMs: Long,
        val responseTimeMs: Long,
        val modelUsed: String,
        val voiceUsed: String
    )

    data class ApiTestResult(
        val isSuccess: Boolean,
        val responseTimeMs: Long,
        val internetPass: Boolean = false,
        val authPass: Boolean = false,
        val modelPass: Boolean = false,
        val ttsPass: Boolean = false,
        val audioPass: Boolean = false,
        val errorCode: Int? = null,
        val errorMessage: String = "",
        val technicalDetails: String? = null,
        val suggestedSolution: String? = null
    )

    // Multi-Speaker Story Analysis Models (Feature 6 & 7 PRD)
    data class StoryAnalysisResult(
        val characters: List<DetectedCharacter>,
        val script: List<ScriptSection>,
        val confidence: Int
    )

    data class DetectedCharacter(
        val name: String,
        val type: String, // NARRATOR, CHARACTER
        val gender: String, // MALE, FEMALE, UNKNOWN
        val age: String, // CHILD, TEENAGER, ADULT, ELDERLY, UNKNOWN
        val personality: String
    )

    data class ScriptSection(
        val speaker: String,
        val text: String,
        val emotion: String
    )

    suspend fun analyzeStory(
        apiKey: String,
        modelId: String,
        text: String
    ): StoryAnalysisResult = withContext(Dispatchers.IO) {
        AppLogger.log("STORY_AI", "Analyzing story for characters and dialogue...")
        
        val systemPrompt = """
            You are a Story Analysis AI. Analyze the provided story/dialogue and extract a structured script.
            Identify all speakers/characters, including the Narrator if applicable.
            Determine character traits like gender and age to help with voice assignment.
            Split the story into sequential sections: each section must have exactly one speaker and their text.
            
            Return ONLY a valid JSON object with this exact structure:
            {
              "confidence": 95,
              "characters": [
                {"name": "Rahul", "type": "CHARACTER", "gender": "MALE", "age": "TEENAGER", "personality": "Friendly"},
                {"name": "Narrator", "type": "NARRATOR", "gender": "UNKNOWN", "age": "UNKNOWN", "personality": "Storyteller"}
              ],
              "script": [
                {"speaker": "Narrator", "text": "Once upon a time...", "emotion": "CALM"},
                {"speaker": "Rahul", "text": "Hello there!", "emotion": "HAPPY"}
              ]
            }
            
            Rules:
            1. Narrator sections cover everything not inside quotes or spoken by a character.
            2. Map every piece of text to a speaker.
            3. Character names must be consistent.
            4. Detect the primary language and preserve the original text exactly.
            5. Confidence is an integer 0-100.
        """.trimIndent()

        val requestJson = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                val userTurn = JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", "$systemPrompt\n\nSTORY TEXT:\n\n$text") })
                    })
                }
                put(userTurn)
            }
            put("contents", contentsArray)
            
            // Force JSON response
            put("generationConfig", JSONObject().apply {
                put("response_mime_type", "application/json")
            })
        }

        val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/"
        val cleanModelId = if (modelId.contains("/")) modelId.split("/").last() else modelId
        val url = "$baseUrl$cleanModelId:generateContent?key=$apiKey"
        
        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val err = response.body?.string() ?: ""
                AppLogger.log("STORY_AI", "Analysis failed: $err")
                throw IOException("AI Analysis failed: ${response.code} ${response.message}")
            }
            val body = response.body?.string() ?: throw IOException("Empty response from AI")
            val json = JSONObject(body)
            val candidates = json.getJSONArray("candidates")
            val content = candidates.getJSONObject(0).getJSONObject("content")
            val parts = content.getJSONArray("parts")
            var responseText = parts.getJSONObject(0).getString("text").trim()
            
            // Cleanup markdown if AI returned it
            if (responseText.startsWith("```json")) {
                responseText = responseText.removePrefix("```json").removeSuffix("```").trim()
            }
            
            val resultJson = JSONObject(responseText)
            
            val characters = mutableListOf<DetectedCharacter>()
            val charArray = resultJson.getJSONArray("characters")
            for (i in 0 until charArray.length()) {
                val c = charArray.getJSONObject(i)
                characters.add(DetectedCharacter(
                    name = c.getString("name"),
                    type = c.optString("type", "CHARACTER"),
                    gender = c.optString("gender", "UNKNOWN"),
                    age = c.optString("age", "UNKNOWN"),
                    personality = c.optString("personality", "NATURAL")
                ))
            }
            
            val script = mutableListOf<ScriptSection>()
            val scriptArray = resultJson.getJSONArray("script")
            for (i in 0 until scriptArray.length()) {
                val s = scriptArray.getJSONObject(i)
                script.add(ScriptSection(
                    speaker = s.getString("speaker"),
                    text = s.getString("text"),
                    emotion = s.optString("emotion", "AUTO")
                ))
            }
            
            StoryAnalysisResult(
                characters = characters,
                script = script,
                confidence = resultJson.optInt("confidence", 80)
            )
        }
    }

    suspend fun generateSpeech(
        apiKey: String,
        modelId: String,
        voiceName: String,
        text: String,
        language: TtsLanguage,
        scene: com.example.data.model.TtsScene? = null,
        customScene: String? = null,
        speakingStyle: SpeakingStyle,
        emotion: VoiceEmotion,
        customEmotion: String? = null,
        emotionIntensity: Float, // 0.0 to 1.0
        customInstruction: String?,
        speed: Float,
        isFastPreview: Boolean = false
    ): TtsGenerationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        AppLogger.log("API", "Initiating speech generation (FastPreview=$isFastPreview) with model: $modelId")

        if (apiKey.isBlank()) {
            throw IllegalArgumentException("API Key is missing. Please add a valid Gemini API key in Key Manager.")
        }
        if (text.isBlank()) {
            throw IllegalArgumentException("Text is empty. Please enter text to convert to speech.")
        }

        // Build prompt with rich natural language guidance
        val promptBuilder = StringBuilder()
        
        if (isFastPreview) {
            // Minimal prompt for low latency voice preview
            promptBuilder.append("Read this preview text with the voice '$voiceName':\n$text")
        } else {
            promptBuilder.append("You are an expressive Text-to-Speech system. Read the following text aloud with high acoustic clarity.\n")
            
            if (!customScene.isNullOrBlank()) {
                promptBuilder.append("Custom Scenario & Context: ${customScene.trim()}\n")
            } else if (scene != null) {
                promptBuilder.append("Scenario Purpose: ${scene.nameEn} (${scene.nameBn}). ${scene.aiContextPrompt}\n")
            }

            if (language != TtsLanguage.AUTO) {
                promptBuilder.append("Target Language: ${language.displayName} (${language.nativeName})\n")
            }
            promptBuilder.append("Speaking Style: ${speakingStyle.displayName}. ${speakingStyle.instruction}\n")

            val intensityDescriptor = when {
                emotionIntensity < 0.33f -> "Subtle"
                emotionIntensity < 0.67f -> "Moderate"
                else -> "Strong"
            }
            if (!customEmotion.isNullOrBlank()) {
                promptBuilder.append("Emotional Tone: ${customEmotion.trim()} with $intensityDescriptor intensity.\n")
            } else {
                promptBuilder.append("Emotional Tone: ${emotion.displayName} (${emotion.promptDescriptor}) with $intensityDescriptor intensity.\n")
            }

            if (speed != 1.0f) {
                promptBuilder.append("Pacing: Read at ${String.format(Locale.US, "%.2f", speed)}x speed.\n")
            }

            if (!customInstruction.isNullOrBlank()) {
                promptBuilder.append("Special Voice Guidance: ${customInstruction.trim()}\n")
            }

            promptBuilder.append("\nText to speak:\n$text")
        }

        // Build Request JSON
        val requestJson = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                val turnObj = JSONObject().apply {
                    put("role", "user")
                    val partsArray = JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", promptBuilder.toString())
                        })
                    }
                    put("parts", partsArray)
                }
                put(turnObj)
            }
            put("contents", contentsArray)

            val genConfig = JSONObject().apply {
                val modalities = JSONArray().apply {
                    put("AUDIO")
                }
                put("responseModalities", modalities)

                val speechConfig = JSONObject().apply {
                    val voiceConfig = JSONObject().apply {
                        val prebuiltVoiceConfig = JSONObject().apply {
                            put("voiceName", voiceName)
                        }
                        put("prebuiltVoiceConfig", prebuiltVoiceConfig)
                    }
                    put("voiceConfig", voiceConfig)
                }
                put("speechConfig", speechConfig)
            }
            put("generationConfig", genConfig)
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent?key=${apiKey.trim()}"
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = requestJson.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        AppLogger.log("API", "Sending request to endpoint...")

        val response = try {
            client.newCall(request).execute()
        } catch (e: IOException) {
            AppLogger.log("API", "Network error: ${e.message}")
            throw IOException("Network error: Could not connect to Google Gemini API. Please check your internet connection.", e)
        }

        val responseTime = System.currentTimeMillis() - startTime
        val responseBodyString = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            handleApiError(response.code, responseBodyString, modelId)
        }

        AppLogger.log("API", "Received HTTP ${response.code} in ${responseTime}ms. Parsing audio...")

        val rootObj = try {
            JSONObject(responseBodyString)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to parse API response JSON: ${e.message}")
        }

        // Extract Candidates
        val candidates = rootObj.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) {
            val promptFeedback = rootObj.optJSONObject("promptFeedback")
            val blockReason = promptFeedback?.optString("blockReason")
            if (!blockReason.isNullOrBlank()) {
                throw IllegalStateException("Text blocked by Gemini safety filter: $blockReason")
            }
            throw IllegalStateException("No speech candidates returned from model. Verify model compatibility.")
        }

        val firstCandidate = candidates.getJSONObject(0)
        val contentObj = firstCandidate.optJSONObject("content")
        val partsArray = contentObj?.optJSONArray("parts")

        var base64AudioData: String? = null
        var audioMimeType = "audio/wav"

        if (partsArray != null) {
            for (i in 0 until partsArray.length()) {
                val part = partsArray.getJSONObject(i)
                val inlineData = part.optJSONObject("inlineData")
                if (inlineData != null) {
                    base64AudioData = inlineData.optString("data")
                    audioMimeType = inlineData.optString("mimeType", "audio/wav")
                    break
                }
            }
        }

        if (base64AudioData.isNullOrBlank()) {
            throw IllegalStateException("Model response did not contain audio data. Model $modelId may not support direct AUDIO modality.")
        }

        val rawAudioBytes = try {
            Base64.decode(base64AudioData, Base64.DEFAULT)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to decode base64 audio payload: ${e.message}")
        }

        if (rawAudioBytes.isEmpty()) {
            throw IllegalStateException("Audio payload was empty.")
        }

        // Save audio to cache
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val cacheDir = File(context.cacheDir, "audio")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val tempFile = File(cacheDir, "KRITY_TTS_${timestamp}.wav")

        WavAudioHelper.saveAudioBytesToFile(
            rawBytes = rawAudioBytes,
            mimeType = audioMimeType,
            outputFile = tempFile
        )

        // Read duration via MediaMetadataRetriever
        var durationMs = 0L
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(tempFile.absolutePath)
            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationMs = durStr?.toLongOrNull() ?: 0L
            retriever.release()
        } catch (e: Exception) {
            // fallback estimation from byte size (e.g. 24kHz 16-bit mono = 48,000 bytes/sec)
            durationMs = (tempFile.length() / 48).coerceAtLeast(1000)
        }

        AppLogger.log("API", "Speech generated successfully: ${tempFile.name}, size: ${tempFile.length()} bytes, duration: ${durationMs}ms")

        TtsGenerationResult(
            audioFile = tempFile,
            mimeType = audioMimeType,
            fileSize = tempFile.length(),
            durationMs = durationMs,
            responseTimeMs = responseTime,
            modelUsed = modelId,
            voiceUsed = voiceName
        )
    }

    suspend fun testApiKeyAndModel(
        apiKey: String,
        modelId: String,
        voiceName: String = "Puck"
    ): ApiTestResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        var internet = false
        var auth = false
        var model = false
        var tts = false
        var audio = false

        // 1. Internet Check
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress("generativelanguage.googleapis.com", 443), 5000)
            socket.close()
            internet = true
        } catch (e: Exception) {
            return@withContext ApiTestResult(
                isSuccess = false,
                responseTimeMs = System.currentTimeMillis() - startTime,
                internetPass = false,
                errorCode = 0,
                errorMessage = "No internet connection or cannot reach Google servers.",
                suggestedSolution = "Check Wi-Fi or mobile data."
            )
        }

        if (apiKey.isBlank()) {
            return@withContext ApiTestResult(
                isSuccess = false,
                responseTimeMs = System.currentTimeMillis() - startTime,
                internetPass = true,
                authPass = false,
                errorCode = 401,
                errorMessage = "API Key is empty.",
                suggestedSolution = "Enter your Gemini API key in Key Manager."
            )
        }

        // Test TTS with short phrase
        val testText = "KRITY AI TTS test. নমস্কার।"
        try {
            val result = generateSpeech(
                apiKey = apiKey,
                modelId = modelId,
                voiceName = voiceName,
                text = testText,
                language = TtsLanguage.ENGLISH,
                speakingStyle = SpeakingStyle.NATURAL,
                emotion = VoiceEmotion.NEUTRAL,
                emotionIntensity = 0.5f,
                customInstruction = null,
                speed = 1.0f
            )

            auth = true
            model = true
            tts = true
            audio = result.fileSize > 100 && result.audioFile.exists()

            ApiTestResult(
                isSuccess = true,
                responseTimeMs = result.responseTimeMs,
                internetPass = true,
                authPass = true,
                modelPass = true,
                ttsPass = true,
                audioPass = audio
            )
        } catch (e: Exception) {
            val respTime = System.currentTimeMillis() - startTime
            val msg = e.message ?: "Unknown error"

            var code: Int? = null
            var cause = "An error occurred during TTS synthesis."
            var solution = "Check the logs and retry."

            when {
                msg.contains("401") || msg.contains("403") || msg.contains("API_KEY_INVALID") -> {
                    code = if (msg.contains("403")) 403 else 401
                    auth = false
                    cause = "Invalid, deactivated, or unauthorized Gemini API key."
                    solution = "Verify your API key in Google AI Studio and update it in Key Manager."
                }
                msg.contains("429") || msg.contains("RESOURCE_EXHAUSTED") -> {
                    code = 429
                    auth = true
                    cause = "Google Gemini rate limit or quota exceeded."
                    solution = "Wait a moment and retry, or add a secondary backup API key with Auto Fallback enabled."
                }
                msg.contains("404") || msg.contains("NOT_FOUND") -> {
                    code = 404
                    auth = true
                    model = false
                    cause = "Model ID '$modelId' was not found or is unavailable."
                    solution = "Select an active model like 'Gemini 2.5 Flash TTS' or configure a valid model ID."
                }
                msg.contains("500") || msg.contains("503") -> {
                    code = 503
                    auth = true
                    model = true
                    cause = "Google Gemini servers are temporarily overloaded."
                    solution = "Wait 30 seconds and retry."
                }
                else -> {
                    cause = msg
                }
            }

            ApiTestResult(
                isSuccess = false,
                responseTimeMs = respTime,
                internetPass = internet,
                authPass = auth,
                modelPass = model,
                ttsPass = tts,
                audioPass = audio,
                errorCode = code,
                errorMessage = msg,
                technicalDetails = e.stackTraceToString().take(300),
                suggestedSolution = solution
            )
        }
    }

    private fun handleApiError(httpCode: Int, responseBody: String, modelId: String) {
        var errorDetails = "HTTP $httpCode"
        var errorCodeStr = ""
        try {
            val json = JSONObject(responseBody)
            val errObj = json.optJSONObject("error")
            if (errObj != null) {
                val msg = errObj.optString("message")
                val status = errObj.optString("status")
                errorCodeStr = status
                errorDetails = if (msg.isNotBlank()) msg else status
            }
        } catch (e: Exception) {
            errorDetails = responseBody.take(150)
        }

        AppLogger.log("API_ERR", "HTTP $httpCode: $errorDetails")

        when (httpCode) {
            401, 403 -> throw IOException("HTTP $httpCode (Auth Error): Invalid or unauthorized API key. Details: $errorDetails")
            404 -> throw IOException("HTTP 404 (Model Not Found): Model '$modelId' does not exist or does not support audio generation. Details: $errorDetails")
            429 -> throw IOException("HTTP 429 (Rate Limit Exceeded): API request limit exceeded or quota exhausted. Please wait and retry or switch key. Details: $errorDetails")
            500, 503 -> throw IOException("HTTP $httpCode (Google Service Temporary Error): Server unavailable. Please retry. Details: $errorDetails")
            else -> throw IOException("HTTP $httpCode: $errorDetails")
        }
    }
}
