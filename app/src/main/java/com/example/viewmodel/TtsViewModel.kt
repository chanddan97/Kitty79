package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiTtsService
import com.example.data.local.ApiKeyPreferences
import com.example.data.local.AppDatabase
import com.example.data.local.ChapterEntity
import com.example.data.local.HistoryEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.StoryScriptSectionEntity
import com.example.data.local.StorySpeakerEntity
import com.example.data.local.UsageEntity
import com.example.data.model.ApiKeyItem
import com.example.data.model.DefaultModelPresets
import com.example.data.model.DiagnosticComponentResult
import com.example.data.model.FullDiagnosticSummary
import com.example.data.model.GeminiModelConfig
import com.example.data.model.KeyStatus
import com.example.data.model.ModelStatus
import com.example.data.model.SpeakingStyle
import com.example.data.model.TtsLanguage
import com.example.data.model.VoiceEmotion
import com.example.data.model.VoiceOption
import com.example.util.AppLogger
import com.example.util.AudioPlayerManager
import com.example.util.DiagnosticsManager
import com.example.util.LanguageDetector
import com.example.util.SmartTextProcessor
import com.example.util.TextChunker
import com.example.util.WavAudioHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TtsViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication<Application>().applicationContext
    val prefs = ApiKeyPreferences(context)
    val database = AppDatabase.getDatabase(context)
    val ttsService = GeminiTtsService(context)
    val playerManager = AudioPlayerManager(context)
    val diagnosticsManager = DiagnosticsManager(context, ttsService, database)

    // Text Input & Analysis
    private val _textInput = MutableStateFlow("")
    val textInput: StateFlow<String> = _textInput.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(TtsLanguage.AUTO)
    val selectedLanguage: StateFlow<TtsLanguage> = _selectedLanguage.asStateFlow()

    val textAnalysis: StateFlow<LanguageDetector.TextAnalysis> = combine(_textInput, _selectedLanguage) { text, lang ->
        LanguageDetector.analyze(text, lang)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        LanguageDetector.analyze("", TtsLanguage.AUTO)
    )

    // Smart Text Processor (Feature 1 PRD)
    val smartProcessedText: StateFlow<SmartTextProcessor.ProcessedTextResult> = combine(_textInput, _selectedLanguage) { text, lang ->
        SmartTextProcessor.process(text, lang)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SmartTextProcessor.process("", TtsLanguage.AUTO)
    )

    private val _showProcessedTextPreview = MutableStateFlow(false)
    val showProcessedTextPreview: StateFlow<Boolean> = _showProcessedTextPreview.asStateFlow()

    fun showProcessedPreview() {
        _showProcessedTextPreview.value = true
    }

    fun dismissProcessedPreview() {
        _showProcessedTextPreview.value = false
    }

    // Models & Selection
    private val _allModels = MutableStateFlow<List<GeminiModelConfig>>(emptyList())
    val allModels: StateFlow<List<GeminiModelConfig>> = _allModels.asStateFlow()

    private val _selectedModelId = MutableStateFlow(prefs.getSelectedModelId())
    val selectedModelId: StateFlow<String> = _selectedModelId.asStateFlow()

    // Voice & Style Controls
    val availableVoices = DefaultModelPresets.voices
    val availableScenes = DefaultModelPresets.scenes

    private val _selectedScene = MutableStateFlow(DefaultModelPresets.scenes.first())
    val selectedScene: StateFlow<com.example.data.model.TtsScene> = _selectedScene.asStateFlow()

    private val _selectedVoiceName = MutableStateFlow(prefs.getSelectedVoice())
    val selectedVoiceName: StateFlow<String> = _selectedVoiceName.asStateFlow()

    private val _selectedStyle = MutableStateFlow(SpeakingStyle.NATURAL)
    val selectedStyle: StateFlow<SpeakingStyle> = _selectedStyle.asStateFlow()

    private val _selectedEmotion = MutableStateFlow(VoiceEmotion.NEUTRAL)
    val selectedEmotion: StateFlow<VoiceEmotion> = _selectedEmotion.asStateFlow()

    private val _customEmotionText = MutableStateFlow("")
    val customEmotionText: StateFlow<String> = _customEmotionText.asStateFlow()

    private val _customSceneText = MutableStateFlow("")
    val customSceneText: StateFlow<String> = _customSceneText.asStateFlow()

    private val _emotionIntensity = MutableStateFlow(0.5f)
    val emotionIntensity: StateFlow<Float> = _emotionIntensity.asStateFlow()

    private val _customInstruction = MutableStateFlow("")
    val customInstruction: StateFlow<String> = _customInstruction.asStateFlow()

    private val _selectedSpeed = MutableStateFlow(prefs.getSelectedSpeed())
    val selectedSpeed: StateFlow<Float> = _selectedSpeed.asStateFlow()

    // Generation State
    enum class GenStep {
        IDLE, PREPARING, CONNECTING, GENERATING, PROCESSING, SUCCESS, ERROR
    }

    private val _genStep = MutableStateFlow(GenStep.IDLE)
    val genStep: StateFlow<GenStep> = _genStep.asStateFlow()

    private val _progressMessage = MutableStateFlow("")
    val progressMessage: StateFlow<String> = _progressMessage.asStateFlow()

    private val _lastGeneratedResult = MutableStateFlow<GeminiTtsService.TtsGenerationResult?>(null)
    val lastGeneratedResult: StateFlow<GeminiTtsService.TtsGenerationResult?> = _lastGeneratedResult.asStateFlow()

    private val _generationError = MutableStateFlow<String?>(null)
    val generationError: StateFlow<String?> = _generationError.asStateFlow()

    // Long Text Chunking Prompt Dialog State
    private val _showChunkDialog = MutableStateFlow(false)
    val showChunkDialog: StateFlow<Boolean> = _showChunkDialog.asStateFlow()

    // API Keys State
    private val _apiKeys = MutableStateFlow<List<ApiKeyItem>>(emptyList())
    val apiKeys: StateFlow<List<ApiKeyItem>> = _apiKeys.asStateFlow()

    private val _autoFallback = MutableStateFlow(prefs.isAutoFallbackEnabled())
    val autoFallback: StateFlow<Boolean> = _autoFallback.asStateFlow()

    private val _testKeyResult = MutableStateFlow<GeminiTtsService.ApiTestResult?>(null)
    val testKeyResult: StateFlow<GeminiTtsService.ApiTestResult?> = _testKeyResult.asStateFlow()

    private val _isTestingKey = MutableStateFlow(false)
    val isTestingKey: StateFlow<Boolean> = _isTestingKey.asStateFlow()

    // Settings
    private val _themeMode = MutableStateFlow(prefs.getThemeMode())
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _autoPlay = MutableStateFlow(prefs.isAutoPlayEnabled())
    val autoPlay: StateFlow<Boolean> = _autoPlay.asStateFlow()

    // History & Search
    private val _historySearchQuery = MutableStateFlow("")
    val historySearchQuery: StateFlow<String> = _historySearchQuery.asStateFlow()

    val historyList: StateFlow<List<HistoryEntity>> = combine(
        database.historyDao().getAllFlow(),
        _historySearchQuery
    ) { list, query ->
        if (query.isBlank()) list
        else list.filter { it.title.contains(query, ignoreCase = true) || it.fullText.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Usage Statistics
    private val todayDateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val todayUsageList: StateFlow<List<UsageEntity>> = database.usageDao().getTodayFlow(todayDateKey)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Projects & Chapter Generator (Features 6 & 9)
    val allProjects: StateFlow<List<ProjectEntity>> = database.projectDao().getAllProjectsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeProjectId = MutableStateFlow<Long?>(null)
    val activeProjectId: StateFlow<Long?> = _activeProjectId.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeProjectChapters: StateFlow<List<ChapterEntity>> = _activeProjectId.flatMapLatest { id ->
        if (id != null) database.projectDao().getChaptersForProjectFlow(id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isGeneratingChapters = MutableStateFlow(false)
    val isGeneratingChapters: StateFlow<Boolean> = _isGeneratingChapters.asStateFlow()

    private val _isChapterPaused = MutableStateFlow(false)
    val isChapterPaused: StateFlow<Boolean> = _isChapterPaused.asStateFlow()

    private val _activeGeneratingChapterId = MutableStateFlow<Long?>(null)
    val activeGeneratingChapterId: StateFlow<Long?> = _activeGeneratingChapterId.asStateFlow()

    private val _unfinishedProjectToResume = MutableStateFlow<ProjectEntity?>(null)
    val unfinishedProjectToResume: StateFlow<ProjectEntity?> = _unfinishedProjectToResume.asStateFlow()

    // Multi-Speaker Story (Feature 10 PRD)
    private val _isAnalyzingStory = MutableStateFlow(false)
    val isAnalyzingStory: StateFlow<Boolean> = _isAnalyzingStory.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeProjectSpeakers: StateFlow<List<StorySpeakerEntity>> = _activeProjectId.flatMapLatest { id ->
        if (id != null) database.projectDao().getSpeakersForProjectFlow(id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeProjectScriptSections: StateFlow<List<StoryScriptSectionEntity>> = _activeProjectId.flatMapLatest { id ->
        if (id != null) database.projectDao().getScriptSectionsForProjectFlow(id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _multiSpeakerGenerationProgress = MutableStateFlow(0f)
    val multiSpeakerGenerationProgress: StateFlow<Float> = _multiSpeakerGenerationProgress.asStateFlow()

    private val _combinedAudioResult = MutableStateFlow<File?>(null)
    val combinedAudioResult: StateFlow<File?> = _combinedAudioResult.asStateFlow()

    // Diagnostics
    val diagnosticSummary: StateFlow<FullDiagnosticSummary> = diagnosticsManager.diagnosticSummary
    private val _diagnosticProgress = MutableStateFlow("")
    val diagnosticProgress: StateFlow<String> = _diagnosticProgress.asStateFlow()

    private val _selectedDiagnosticDetail = MutableStateFlow<DiagnosticComponentResult?>(null)
    val selectedDiagnosticDetail: StateFlow<DiagnosticComponentResult?> = _selectedDiagnosticDetail.asStateFlow()

    // Player State forwarded
    val playerState = playerManager.playerState

    // Logs
    val logsFlow = AppLogger.logsFlow

    init {
        refreshModels()
        refreshApiKeys()
        checkUnfinishedGeneration()
        AppLogger.log("INIT", "KRITY AI TTS initialized successfully.")
    }

    private fun checkUnfinishedGeneration() {
        viewModelScope.launch {
            val projects = database.projectDao().getAllProjectsFlow().firstOrNull() ?: emptyList()
            val unfinished = projects.firstOrNull { it.status == "GENERATING" || it.status == "PAUSED" }
            if (unfinished != null && unfinished.completedChapters < unfinished.totalChapters) {
                _unfinishedProjectToResume.value = unfinished
                AppLogger.log("PROJECT", "Found unfinished generation: ${unfinished.title}")
            }
        }
    }

    fun onTextChanged(newText: String) {
        _textInput.value = newText
    }

    fun clearText() {
        _textInput.value = ""
    }

    fun selectLanguage(language: TtsLanguage) {
        _selectedLanguage.value = language
    }

    fun selectModel(modelId: String) {
        _selectedModelId.value = modelId
        prefs.setSelectedModelId(modelId)
        AppLogger.log("SETTINGS", "Selected model changed to: $modelId")
    }

    fun selectScene(scene: com.example.data.model.TtsScene) {
        _selectedScene.value = scene
        _selectedStyle.value = scene.recommendedStyle
        _selectedEmotion.value = scene.recommendedEmotion
        AppLogger.log("SCENE", "Selected scene changed to: ${scene.nameEn} (${scene.nameBn})")
    }

    fun selectVoice(voiceName: String) {
        _selectedVoiceName.value = voiceName
        prefs.setSelectedVoice(voiceName)
        AppLogger.log("SETTINGS", "Selected voice changed to: $voiceName")
    }

    fun selectStyle(style: SpeakingStyle) {
        _selectedStyle.value = style
    }

    fun selectEmotion(emotion: VoiceEmotion) {
        _selectedEmotion.value = emotion
    }

    fun setCustomEmotionText(emotionText: String) {
        _customEmotionText.value = emotionText
    }

    fun setCustomSceneText(sceneText: String) {
        _customSceneText.value = sceneText
    }

    fun setEmotionIntensity(intensity: Float) {
        _emotionIntensity.value = intensity.coerceIn(0.1f, 1.0f)
    }

    fun setCustomInstruction(instruction: String) {
        _customInstruction.value = instruction
    }

    fun selectSpeed(speed: Float) {
        _selectedSpeed.value = speed
        prefs.setSelectedSpeed(speed)
        playerManager.setPlaybackSpeed(speed)
    }

    fun toggleAutoPlay(enabled: Boolean) {
        _autoPlay.value = enabled
        prefs.setAutoPlayEnabled(enabled)
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        prefs.setThemeMode(mode)
    }

    fun toggleAutoFallback(enabled: Boolean) {
        _autoFallback.value = enabled
        prefs.setAutoFallbackEnabled(enabled)
    }

    private fun refreshModels() {
        val custom = prefs.getCustomModels()
        val all = DefaultModelPresets.models + custom
        _allModels.value = all
    }

    fun addCustomModel(id: String, displayName: String, description: String) {
        val currentCustom = prefs.getCustomModels().toMutableList()
        currentCustom.add(
            GeminiModelConfig(
                id = id.trim(),
                displayName = displayName.trim(),
                description = description.trim(),
                isCustom = true
            )
        )
        prefs.saveCustomModels(currentCustom)
        refreshModels()
        selectModel(id.trim())
        AppLogger.log("MODEL", "Added custom model: $id")
    }

    fun refreshApiKeys() {
        _apiKeys.value = prefs.getApiKeys()
    }

    fun addApiKey(label: String, key: String) {
        val current = prefs.getApiKeys().toMutableList()
        val isFirst = current.isEmpty()
        val newItem = ApiKeyItem(
            label = label.trim().ifEmpty { "Personal Key" },
            key = key.trim(),
            isPrimary = isFirst,
            isEnabled = true,
            status = KeyStatus.UNTESTED
        )
        current.add(newItem)
        prefs.saveApiKeys(current)
        refreshApiKeys()
        AppLogger.log("API_KEY", "Added new API key: ${newItem.label} (${newItem.maskedKey}). Auto testing now...")
        // Automatically perform live test immediately
        testSingleKey(newItem)
    }

    fun updateApiKey(id: String, label: String, key: String) {
        var updatedItem: ApiKeyItem? = null
        val current = prefs.getApiKeys().map {
            if (it.id == id) {
                val item = it.copy(
                    label = label.trim(),
                    key = key.trim(),
                    status = KeyStatus.UNTESTED,
                    lastError = null
                )
                updatedItem = item
                item
            } else it
        }
        prefs.saveApiKeys(current)
        refreshApiKeys()
        AppLogger.log("API_KEY", "Updated API key: $label. Auto testing now...")
        updatedItem?.let { testSingleKey(it) }
    }

    fun deleteApiKey(id: String) {
        val current = prefs.getApiKeys().filterNot { it.id == id }
        // Ensure at least one is primary if list not empty
        if (current.isNotEmpty() && current.none { it.isPrimary }) {
            current.first().let { prefs.setPrimaryKey(it.id) }
        }
        prefs.saveApiKeys(current)
        refreshApiKeys()
        AppLogger.log("API_KEY", "Deleted API key: $id")
    }

    fun setPrimaryKey(id: String) {
        prefs.setPrimaryKey(id)
        refreshApiKeys()
    }

    fun toggleApiKeyEnabled(id: String) {
        val current = prefs.getApiKeys().map {
            if (it.id == id) it.copy(isEnabled = !it.isEnabled) else it
        }
        prefs.saveApiKeys(current)
        refreshApiKeys()
    }

    fun testSingleKey(keyItem: ApiKeyItem) {
        viewModelScope.launch {
            _isTestingKey.value = true
            _testKeyResult.value = null
            AppLogger.log("TEST", "Starting live key test for: ${keyItem.label}")

            val result = ttsService.testApiKeyAndModel(
                apiKey = keyItem.key,
                modelId = _selectedModelId.value,
                voiceName = _selectedVoiceName.value
            )

            val status = when {
                result.isSuccess -> KeyStatus.WORKING
                result.errorCode == 429 -> KeyStatus.RATE_LIMITED
                result.errorCode == 401 || result.errorCode == 403 -> KeyStatus.INVALID
                else -> KeyStatus.ERROR
            }

            prefs.updateKeyStatus(keyItem.id, status, result.responseTimeMs, result.errorMessage)
            refreshApiKeys()
            _testKeyResult.value = result
            _isTestingKey.value = false
            AppLogger.log("TEST", "Key test finished: status=$status, time=${result.responseTimeMs}ms")
        }
    }

    fun dismissTestKeyResult() {
        _testKeyResult.value = null
    }

    // Voice Preview
    fun previewVoice(voice: VoiceOption) {
        viewModelScope.launch {
            val key = prefs.getPrimaryApiKey()
            if (key == null) {
                _generationError.value = "Please add a Gemini API key first to preview voices."
                return@launch
            }

            AppLogger.log("VOICE_PREVIEW", "Previewing voice: ${voice.name}")
            try {
                val result = ttsService.generateSpeech(
                    apiKey = key.key,
                    modelId = _selectedModelId.value,
                    voiceName = voice.name,
                    text = voice.previewText,
                    language = TtsLanguage.AUTO,
                    speakingStyle = SpeakingStyle.NATURAL,
                    emotion = VoiceEmotion.NEUTRAL,
                    emotionIntensity = 0.5f,
                    customInstruction = null,
                    speed = 1.0f,
                    isFastPreview = true
                )
                playerManager.playFile(result.audioFile, 1.0f)
            } catch (e: Exception) {
                AppLogger.log("VOICE_PREVIEW", "Voice preview failed: ${e.message}")
            }
        }
    }

    // Trigger Generation (with chunk check)
    fun onGenerateClicked() {
        val text = _textInput.value.trim()
        if (text.isBlank()) {
            _generationError.value = "Please enter some text to generate speech."
            return
        }

        if (TextChunker.shouldChunk(text)) {
            _showChunkDialog.value = true
        } else {
            generateSpeechInternal(listOf(text))
        }
    }

    fun onDismissChunkDialog() {
        _showChunkDialog.value = false
    }

    fun onProceedWithChunking(split: Boolean) {
        _showChunkDialog.value = false
        val text = _textInput.value.trim()
        val chunks = if (split) TextChunker.splitIntoChunks(text) else listOf(text)
        generateSpeechInternal(chunks)
    }

    private fun generateSpeechInternal(chunks: List<String>) {
        viewModelScope.launch {
            _generationError.value = null
            _genStep.value = GenStep.PREPARING
            _progressMessage.value = "Preparing synthesis pipeline..."
            AppLogger.log("GENERATE", "Starting speech generation (${chunks.size} chunk(s))...")

            val keys = prefs.getApiKeys().filter { it.isEnabled }
            if (keys.isEmpty()) {
                _genStep.value = GenStep.ERROR
                _generationError.value = "No active Gemini API key found. Please configure an API key in Key Manager."
                return@launch
            }

            // Pre-flight check: Internet Connectivity (Feature 7 PRD)
            val isOnline = withContext(Dispatchers.IO) {
                try {
                    val s = java.net.Socket()
                    s.connect(java.net.InetSocketAddress("generativelanguage.googleapis.com", 443), 4000)
                    s.close()
                    true
                } catch (e: Exception) {
                    false
                }
            }
            if (!isOnline) {
                _genStep.value = GenStep.ERROR
                _generationError.value = "No Internet Connection: Cannot reach Google Gemini servers. Previously generated audio and local projects remain fully accessible."
                AppLogger.log("PREFLIGHT", "Aborted: Device is offline.")
                return@launch
            }

            val primaryKey = keys.firstOrNull { it.isPrimary } ?: keys.first()
            val candidateKeys = if (_autoFallback.value) {
                listOf(primaryKey) + keys.filter { it.id != primaryKey.id }
            } else {
                listOf(primaryKey)
            }

            var generatedResult: GeminiTtsService.TtsGenerationResult? = null
            var lastErr: Exception? = null

            for ((keyIndex, keyItem) in candidateKeys.withIndex()) {
                try {
                    _genStep.value = GenStep.CONNECTING
                    _progressMessage.value = "Connecting to Gemini (${keyItem.label})..."
                    delay(200)

                    _genStep.value = GenStep.GENERATING
                    _progressMessage.value = "Generating natural AI voice..."

                    val combinedText = chunks.joinToString("\n\n")

                    val result = ttsService.generateSpeech(
                        apiKey = keyItem.key,
                        modelId = _selectedModelId.value,
                        voiceName = _selectedVoiceName.value,
                        text = combinedText,
                        language = _selectedLanguage.value,
                        scene = _selectedScene.value,
                        customScene = _customSceneText.value.takeIf { it.isNotBlank() },
                        speakingStyle = _selectedStyle.value,
                        emotion = _selectedEmotion.value,
                        customEmotion = _customEmotionText.value.takeIf { it.isNotBlank() },
                        emotionIntensity = _emotionIntensity.value,
                        customInstruction = _customInstruction.value,
                        speed = _selectedSpeed.value
                    )

                    _genStep.value = GenStep.PROCESSING
                    _progressMessage.value = "Processing and decoding audio..."
                    delay(150)

                    generatedResult = result

                    // Record success in usage database
                    recordUsage(
                        modelId = _selectedModelId.value,
                        voiceName = _selectedVoiceName.value,
                        charCount = combinedText.length,
                        isSuccess = true,
                        responseTimeMs = result.responseTimeMs,
                        estimatedDurationMs = result.durationMs
                    )

                    // Record in History database
                    val historyTitle = combinedText.take(60).replace("\n", " ").trim()
                    val historyItem = HistoryEntity(
                        title = historyTitle,
                        fullText = combinedText,
                        modelId = _selectedModelId.value,
                        modelName = _allModels.value.firstOrNull { it.id == _selectedModelId.value }?.displayName ?: _selectedModelId.value,
                        voiceName = _selectedVoiceName.value,
                        language = _selectedLanguage.value.displayName,
                        style = _selectedStyle.value.displayName,
                        emotion = _selectedEmotion.value.displayName,
                        speed = _selectedSpeed.value,
                        durationMs = result.durationMs,
                        audioFilePath = result.audioFile.absolutePath,
                        fileSize = result.fileSize
                    )
                    database.historyDao().insert(historyItem)

                    prefs.updateKeyStatus(keyItem.id, KeyStatus.WORKING, result.responseTimeMs)
                    break // Succeeded!
                } catch (e: Exception) {
                    lastErr = e
                    AppLogger.log("GENERATE_FAIL", "Key '${keyItem.label}' failed: ${e.message}")

                    val isRateOrAuth = e.message?.contains("429") == true || e.message?.contains("401") == true || e.message?.contains("403") == true
                    val status = if (e.message?.contains("429") == true) KeyStatus.RATE_LIMITED else KeyStatus.ERROR
                    prefs.updateKeyStatus(keyItem.id, status, 0L, e.message)

                    if (_autoFallback.value && keyIndex < candidateKeys.lastIndex && isRateOrAuth) {
                        _progressMessage.value = "Auto fallback: Switching to backup key (${candidateKeys[keyIndex + 1].label})..."
                        AppLogger.log("FALLBACK", "Switching to next key...")
                        delay(600)
                        continue
                    } else {
                        break
                    }
                }
            }

            if (generatedResult != null) {
                _lastGeneratedResult.value = generatedResult
                _genStep.value = GenStep.SUCCESS
                _progressMessage.value = "Voice generation complete!"

                if (_autoPlay.value) {
                    playerManager.playFile(generatedResult.audioFile, _selectedSpeed.value)
                }
            } else {
                _genStep.value = GenStep.ERROR
                val errMessage = lastErr?.message ?: "Speech synthesis failed. Please check diagnostics and API keys."
                _generationError.value = errMessage
                recordUsage(
                    modelId = _selectedModelId.value,
                    voiceName = _selectedVoiceName.value,
                    charCount = _textInput.value.length,
                    isSuccess = false,
                    errorMessage = errMessage
                )
            }
        }
    }

    private suspend fun recordUsage(
        modelId: String,
        voiceName: String,
        charCount: Int,
        isSuccess: Boolean,
        errorCode: Int? = null,
        errorMessage: String? = null,
        responseTimeMs: Long = 0L,
        estimatedDurationMs: Long = 0L
    ) {
        try {
            database.usageDao().insert(
                UsageEntity(
                    dateKey = todayDateKey,
                    modelId = modelId,
                    voiceName = voiceName,
                    characterCount = charCount,
                    isSuccess = isSuccess,
                    errorCode = errorCode,
                    errorMessage = errorMessage,
                    responseTimeMs = responseTimeMs,
                    estimatedAudioDurationMs = estimatedDurationMs
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playCurrentResult() {
        val result = _lastGeneratedResult.value ?: return
        playerManager.playFile(result.audioFile, _selectedSpeed.value)
    }

    fun saveCurrentResult(context: Context): File? {
        val result = _lastGeneratedResult.value ?: return null
        return playerManager.saveAudioToDevice(result.audioFile)
    }

    fun shareCurrentResult(context: Context) {
        val result = _lastGeneratedResult.value ?: return
        playerManager.shareAudio(context, result.audioFile)
    }

    // History Actions
    fun onSearchHistory(query: String) {
        _historySearchQuery.value = query
    }

    fun playHistoryAudio(item: HistoryEntity) {
        val file = File(item.audioFilePath)
        if (file.exists()) {
            playerManager.playFile(file, item.speed)
        } else {
            _generationError.value = "Audio file not found on device: ${file.name}"
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            val item = database.historyDao().getById(id)
            item?.let {
                val f = File(it.audioFilePath)
                if (f.exists()) f.delete()
                database.historyDao().deleteById(id)
                AppLogger.log("HISTORY", "Deleted history entry #$id")
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            database.historyDao().clearAll()
            AppLogger.log("HISTORY", "Cleared all history entries.")
        }
    }

    fun saveHistoryAudio(context: Context, item: HistoryEntity): File? {
        val file = File(item.audioFilePath)
        return if (file.exists()) {
            playerManager.saveAudioToDevice(file)
        } else null
    }

    fun shareHistoryAudio(context: Context, item: HistoryEntity) {
        val file = File(item.audioFilePath)
        if (file.exists()) {
            playerManager.shareAudio(context, file)
        }
    }

    // Diagnostics Actions
    fun runFullDiagnostics() {
        viewModelScope.launch {
            val key = prefs.getPrimaryApiKey()?.key ?: ""
            diagnosticsManager.runFullDiagnostics(
                apiKey = key,
                modelId = _selectedModelId.value,
                voiceName = _selectedVoiceName.value,
                onProgressUpdate = { _diagnosticProgress.value = it }
            )
            _diagnosticProgress.value = ""
        }
    }

    fun selectDiagnosticDetail(result: DiagnosticComponentResult?) {
        _selectedDiagnosticDetail.value = result
    }

    // Projects & Chapter Generator Actions (Features 6 & 9)
    fun createProjectFromText(title: String, rawText: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val trimmedTitle = title.trim().ifEmpty { "Story Project #${System.currentTimeMillis() % 1000}" }
            val processed = SmartTextProcessor.process(rawText, _selectedLanguage.value)
            val chunks = if (processed.chunks.isNotEmpty()) processed.chunks else listOf(rawText)

            val project = ProjectEntity(
                title = trimmedTitle,
                rawText = rawText,
                modelId = _selectedModelId.value,
                voiceName = _selectedVoiceName.value,
                language = _selectedLanguage.value.displayName,
                style = _selectedStyle.value.displayName,
                scene = _selectedScene.value.nameEn,
                emotion = _selectedEmotion.value.displayName,
                status = "DRAFT",
                totalChapters = chunks.size,
                completedChapters = 0
            )

            val projectId = database.projectDao().insertProject(project)

            val chapters = chunks.mapIndexed { index, chunkText ->
                val padIndex = String.format(Locale.US, "%02d", index + 1)
                ChapterEntity(
                    projectId = projectId,
                    chapterIndex = index + 1,
                    title = "Part $padIndex",
                    content = chunkText,
                    status = "WAITING"
                )
            }

            database.projectDao().insertChapters(chapters)
            _activeProjectId.value = projectId
            AppLogger.log("PROJECT", "Created project '$trimmedTitle' with ${chapters.size} parts.")
            onCreated(projectId)
        }
    }

    fun selectProject(projectId: Long) {
        _activeProjectId.value = projectId
    }

    fun startChapterQueue(projectId: Long) {
        viewModelScope.launch {
            val project = database.projectDao().getProjectById(projectId) ?: return@launch
            _activeProjectId.value = projectId
            _isGeneratingChapters.value = true
            _isChapterPaused.value = false

            database.projectDao().updateProject(project.copy(status = "GENERATING", updatedAt = System.currentTimeMillis()))

            val chapters = database.projectDao().getChaptersForProject(projectId)
            val candidateKeys = prefs.getApiKeys().filter { it.isEnabled }
            if (candidateKeys.isEmpty()) {
                _generationError.value = "No active Gemini API key found. Please configure an API key."
                _isGeneratingChapters.value = false
                return@launch
            }

            for (ch in chapters) {
                if (_isChapterPaused.value) {
                    database.projectDao().updateProject(project.copy(status = "PAUSED", updatedAt = System.currentTimeMillis()))
                    AppLogger.log("CHAPTER", "Chapter generation paused by user.")
                    break
                }
                if (ch.status == "COMPLETED") continue // Never regenerate already completed parts unnecessarily!

                _activeGeneratingChapterId.value = ch.id
                database.projectDao().updateChapter(ch.copy(status = "GENERATING", errorMessage = null, updatedAt = System.currentTimeMillis()))

                var chapterSuccess = false
                for (keyItem in candidateKeys) {
                    try {
                        val result = ttsService.generateSpeech(
                            apiKey = keyItem.key,
                            modelId = project.modelId.ifEmpty { _selectedModelId.value },
                            voiceName = project.voiceName.ifEmpty { _selectedVoiceName.value },
                            text = ch.content,
                            language = _selectedLanguage.value,
                            speakingStyle = _selectedStyle.value,
                            emotion = _selectedEmotion.value,
                            emotionIntensity = _emotionIntensity.value,
                            customInstruction = null,
                            speed = _selectedSpeed.value
                        )

                        database.projectDao().updateChapter(
                            ch.copy(
                                status = "COMPLETED",
                                audioFilePath = result.audioFile.absolutePath,
                                durationMs = result.durationMs,
                                errorMessage = null,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                        chapterSuccess = true
                        break
                    } catch (e: Exception) {
                        AppLogger.log("CHAPTER", "Chapter ${ch.chapterIndex} failed on key ${keyItem.label}: ${e.message}")
                    }
                }

                if (!chapterSuccess) {
                    database.projectDao().updateChapter(
                        ch.copy(
                            status = "FAILED",
                            errorMessage = "Generation failed. Tap Retry to attempt again.",
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }

                val updatedChapters = database.projectDao().getChaptersForProject(projectId)
                val completedCount = updatedChapters.count { it.status == "COMPLETED" }
                val hasFailed = updatedChapters.any { it.status == "FAILED" }
                val projectStatus = if (completedCount == updatedChapters.size) "COMPLETED" else if (hasFailed) "ERROR" else "GENERATING"
                database.projectDao().updateProject(
                    project.copy(
                        completedChapters = completedCount,
                        status = projectStatus,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }

            _activeGeneratingChapterId.value = null
            _isGeneratingChapters.value = false
        }
    }

    fun pauseChapterQueue() {
        _isChapterPaused.value = true
        _isGeneratingChapters.value = false
    }

    fun resumeChapterQueue(projectId: Long) {
        _isChapterPaused.value = false
        startChapterQueue(projectId)
    }

    fun cancelChapterQueue(projectId: Long) {
        _isChapterPaused.value = true
        _isGeneratingChapters.value = false
        _activeGeneratingChapterId.value = null
        viewModelScope.launch {
            val project = database.projectDao().getProjectById(projectId)
            project?.let {
                database.projectDao().updateProject(it.copy(status = "PAUSED", updatedAt = System.currentTimeMillis()))
            }
        }
    }

    fun retrySingleChapter(chapterId: Long) {
        viewModelScope.launch {
            val ch = database.projectDao().getChapterById(chapterId) ?: return@launch
            val project = database.projectDao().getProjectById(ch.projectId) ?: return@launch
            val candidateKeys = prefs.getApiKeys().filter { it.isEnabled }
            if (candidateKeys.isEmpty()) {
                _generationError.value = "No active Gemini API key found."
                return@launch
            }

            _activeGeneratingChapterId.value = ch.id
            database.projectDao().updateChapter(ch.copy(status = "GENERATING", errorMessage = null, updatedAt = System.currentTimeMillis()))

            var success = false
            for (keyItem in candidateKeys) {
                try {
                    val result = ttsService.generateSpeech(
                        apiKey = keyItem.key,
                        modelId = project.modelId.ifEmpty { _selectedModelId.value },
                        voiceName = project.voiceName.ifEmpty { _selectedVoiceName.value },
                        text = ch.content,
                        language = _selectedLanguage.value,
                        speakingStyle = _selectedStyle.value,
                        emotion = _selectedEmotion.value,
                        emotionIntensity = _emotionIntensity.value,
                        customInstruction = null,
                        speed = _selectedSpeed.value
                    )
                    database.projectDao().updateChapter(
                        ch.copy(
                            status = "COMPLETED",
                            audioFilePath = result.audioFile.absolutePath,
                            durationMs = result.durationMs,
                            errorMessage = null,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    success = true
                    break
                } catch (e: Exception) {
                    AppLogger.log("CHAPTER_RETRY", "Failed: ${e.message}")
                }
            }

            if (!success) {
                database.projectDao().updateChapter(
                    ch.copy(status = "FAILED", errorMessage = "Retry failed. Please check network/key.", updatedAt = System.currentTimeMillis())
                )
            }

            val updatedChapters = database.projectDao().getChaptersForProject(project.id)
            val completedCount = updatedChapters.count { it.status == "COMPLETED" }
            val hasFailed = updatedChapters.any { it.status == "FAILED" }
            database.projectDao().updateProject(
                project.copy(
                    completedChapters = completedCount,
                    status = if (completedCount == updatedChapters.size) "COMPLETED" else if (hasFailed) "ERROR" else "PAUSED",
                    updatedAt = System.currentTimeMillis()
                )
            )
            _activeGeneratingChapterId.value = null
        }
    }

    fun playChapterAudio(chapter: ChapterEntity) {
        val path = chapter.audioFilePath ?: return
        val file = File(path)
        if (file.exists()) {
            playerManager.playFile(file, 1.0f)
        } else {
            _generationError.value = "Audio file for ${chapter.title} not found on device."
        }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            val chapters = database.projectDao().getChaptersForProject(projectId)
            chapters.forEach { ch ->
                ch.audioFilePath?.let { File(it).delete() }
            }
            database.projectDao().deleteChaptersForProject(projectId)
            database.projectDao().deleteProject(projectId)
            if (_activeProjectId.value == projectId) {
                _activeProjectId.value = null
            }
            AppLogger.log("PROJECT", "Deleted project #$projectId")
        }
    }

    fun renameProject(projectId: Long, newTitle: String) {
        viewModelScope.launch {
            val project = database.projectDao().getProjectById(projectId) ?: return@launch
            database.projectDao().updateProject(project.copy(title = newTitle.trim(), updatedAt = System.currentTimeMillis()))
        }
    }

    fun dismissResumePrompt() {
        _unfinishedProjectToResume.value = null
    }

    fun startOverProject(projectId: Long) {
        viewModelScope.launch {
            val project = database.projectDao().getProjectById(projectId) ?: return@launch
            val chapters = database.projectDao().getChaptersForProject(projectId)
            chapters.forEach { ch ->
                ch.audioFilePath?.let { File(it).delete() }
                database.projectDao().updateChapter(ch.copy(status = "WAITING", audioFilePath = null, errorMessage = null))
            }
            database.projectDao().updateProject(project.copy(status = "DRAFT", completedChapters = 0, updatedAt = System.currentTimeMillis()))
            _unfinishedProjectToResume.value = null
            startChapterQueue(projectId)
        }
    }

    // Multi-Speaker Story Actions (Feature 10 PRD)
    fun createMultiSpeakerProject(title: String, rawText: String, onComplete: (Long) -> Unit) {
        viewModelScope.launch {
            val key = prefs.getPrimaryApiKey()?.key
            if (key == null) {
                _generationError.value = "Please add an API key first to use AI Story Studio."
                return@launch
            }
            
            _isAnalyzingStory.value = true
            AppLogger.log("STORY", "Starting AI analysis for multi-speaker story...")
            
            try {
                val analysis = ttsService.analyzeStory(key, _selectedModelId.value, rawText)
                
                val project = ProjectEntity(
                    title = title.trim().ifEmpty { "AI Story Production #${System.currentTimeMillis() % 1000}" },
                    rawText = rawText,
                    modelId = _selectedModelId.value,
                    voiceName = "Multi-Voice",
                    language = "Auto",
                    style = "Cinematic",
                    scene = "Story Studio",
                    emotion = "AI Guided",
                    projectType = "MULTI_SPEAKER",
                    status = "DRAFT"
                )
                val projectId = database.projectDao().insertProject(project)
                
                // Map and assign voices
                val speakerMap = mutableMapOf<String, Long>()
                analysis.characters.forEachIndexed { index, char ->
                    // Intelligent auto voice assignment (balanced selection)
                    val voice = if (char.type == "NARRATOR") {
                        availableVoices.firstOrNull { it.name == "Charli" } ?: availableVoices.first()
                    } else {
                        // Maximize voice distinction by picking from pool
                        val pool = if (char.gender == "FEMALE") availableVoices.filter { it.gender == "Female" }
                                   else if (char.gender == "MALE") availableVoices.filter { it.gender == "Male" }
                                   else availableVoices
                        
                        val selectedVoice = if (pool.isNotEmpty()) pool[index % pool.size] else availableVoices.random()
                        selectedVoice
                    }

                    val speakerId = database.projectDao().insertSpeaker(StorySpeakerEntity(
                        projectId = projectId,
                        name = char.name,
                        speakerType = char.type,
                        voiceName = voice.name,
                        gender = char.gender,
                        age = char.age,
                        personality = char.personality
                    ))
                    speakerMap[char.name] = speakerId
                }
                
                // Save script sections
                val sections = analysis.script.mapIndexed { index, s ->
                    StoryScriptSectionEntity(
                        projectId = projectId,
                        sectionIndex = index,
                        speakerId = speakerMap[s.speaker],
                        speakerName = s.speaker,
                        text = s.text,
                        emotion = s.emotion
                    )
                }
                database.projectDao().insertScriptSections(sections)
                
                _activeProjectId.value = projectId
                AppLogger.log("STORY", "Story analysis complete. Detected ${analysis.characters.size} speakers and ${sections.size} dialogue parts.")
                onComplete(projectId)
            } catch (e: Exception) {
                AppLogger.log("STORY", "Analysis failed: ${e.message}")
                _generationError.value = "AI Analysis Failed: ${e.message}. Try a shorter story or check your API key."
            } finally {
                _isAnalyzingStory.value = false
            }
        }
    }

    fun updateSpeakerVoice(speakerId: Long, voiceName: String) {
        viewModelScope.launch {
            val speakers = activeProjectSpeakers.value
            val speaker = speakers.firstOrNull { it.id == speakerId }
            if (speaker != null) {
                database.projectDao().updateSpeaker(speaker.copy(voiceName = voiceName, updatedAt = System.currentTimeMillis()))
                AppLogger.log("SPEAKER", "Updated voice for ${speaker.name} to $voiceName")
            }
        }
    }

    fun startMultiSpeakerGeneration(projectId: Long) {
        viewModelScope.launch {
            val project = database.projectDao().getProjectById(projectId) ?: return@launch
            val speakers = database.projectDao().getSpeakersForProject(projectId)
            val sections = database.projectDao().getScriptSectionsForProject(projectId)
            val speakerMap = speakers.associateBy { it.id }
            
            val candidateKeys = prefs.getApiKeys().filter { it.isEnabled }
            if (candidateKeys.isEmpty()) {
                _generationError.value = "No active Gemini API key found."
                return@launch
            }

            _isGeneratingChapters.value = true
            _isChapterPaused.value = false
            _multiSpeakerGenerationProgress.value = 0f
            
            database.projectDao().updateProject(project.copy(status = "GENERATING"))

            val generatedFiles = mutableListOf<File>()
            var successCount = 0
            
            for ((index, section) in sections.withIndex()) {
                if (_isChapterPaused.value) break
                if (section.status == "COMPLETED" && section.audioFilePath != null) {
                    generatedFiles.add(File(section.audioFilePath))
                    successCount++
                    continue
                }

                database.projectDao().updateScriptSection(section.copy(status = "GENERATING"))
                
                val speaker = speakerMap[section.speakerId]
                val voiceToUse = speaker?.voiceName ?: availableVoices.first().name
                
                var sectionSuccess = false
                for (keyItem in candidateKeys) {
                    try {
                        val res = ttsService.generateSpeech(
                            apiKey = keyItem.key,
                            modelId = project.modelId,
                            voiceName = voiceToUse,
                            text = section.text,
                            language = TtsLanguage.AUTO,
                            speakingStyle = SpeakingStyle.NATURAL,
                            emotion = VoiceEmotion.NEUTRAL,
                            customEmotion = if (section.emotion != "AUTO") section.emotion else null,
                            emotionIntensity = 0.6f,
                            customInstruction = "Character context: ${speaker?.personality ?: "Natural"}. Speaking as: ${speaker?.name ?: "Narrator"}.",
                            speed = 1.0f
                        )
                        
                        database.projectDao().updateScriptSection(section.copy(
                            status = "COMPLETED",
                            audioFilePath = res.audioFile.absolutePath,
                            durationMs = res.durationMs
                        ))
                        generatedFiles.add(res.audioFile)
                        sectionSuccess = true
                        break
                    } catch (e: Exception) {
                        AppLogger.log("STORY_GEN", "Part $index failed on key ${keyItem.label}: ${e.message}")
                        delay(500)
                    }
                }

                if (!sectionSuccess) {
                    database.projectDao().updateScriptSection(section.copy(status = "FAILED", errorMessage = "Failed to generate audio for this part."))
                } else {
                    successCount++
                }
                
                _multiSpeakerGenerationProgress.value = (index + 1).toFloat() / sections.size.toFloat()
                
                // Feature 48 PRD: Automatic Rate Limit Protection Delay
                delay(800) 
            }

            // Assemble Final Audio (Feature 31 & 58 PRD)
            if (successCount == sections.size && generatedFiles.isNotEmpty()) {
                val outputDir = File(context.filesDir, "combined_stories")
                if (!outputDir.exists()) outputDir.mkdirs()
                val finalFile = File(outputDir, "Story_${project.id}_${System.currentTimeMillis()}.wav")
                
                withContext(Dispatchers.IO) {
                    WavAudioHelper.joinWavFiles(generatedFiles, finalFile)
                }
                
                _combinedAudioResult.value = finalFile
                database.projectDao().updateProject(project.copy(status = "COMPLETED"))
                AppLogger.log("STORY", "Successfully assembled continuous story audio: ${finalFile.name}")
            } else {
                database.projectDao().updateProject(project.copy(status = "ERROR"))
            }

            _isGeneratingChapters.value = false
        }
    }

    fun dismissError() {
        _generationError.value = null
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
