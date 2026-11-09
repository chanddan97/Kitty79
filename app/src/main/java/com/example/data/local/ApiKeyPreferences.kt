package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.example.data.model.ApiKeyItem
import com.example.data.model.GeminiModelConfig
import com.example.data.model.KeyStatus
import org.json.JSONArray
import org.json.JSONObject

class ApiKeyPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("krity_tts_api_keys", Context.MODE_PRIVATE)

    init {
        // Initialize with BuildConfig key if available and no keys exist yet
        val existingKeys = getApiKeys()
        if (existingKeys.isEmpty()) {
            val buildKey = try {
                BuildConfig.GEMINI_API_KEY
            } catch (e: Throwable) {
                ""
            }
            if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") {
                val defaultKey = ApiKeyItem(
                    label = "Project Configured Key",
                    key = buildKey.trim(),
                    isPrimary = true,
                    isEnabled = true,
                    status = KeyStatus.UNTESTED
                )
                saveApiKeys(listOf(defaultKey))
            }
        }
    }

    fun getApiKeys(): List<ApiKeyItem> {
        val jsonString = prefs.getString(KEY_API_KEYS_LIST, null) ?: return emptyList()
        val list = mutableListOf<ApiKeyItem>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ApiKeyItem(
                        id = obj.getString("id"),
                        label = obj.getString("label"),
                        key = obj.getString("key"),
                        isPrimary = obj.optBoolean("isPrimary", false),
                        isEnabled = obj.optBoolean("isEnabled", true),
                        status = try {
                            KeyStatus.valueOf(obj.optString("status", KeyStatus.UNTESTED.name))
                        } catch (e: Exception) {
                            KeyStatus.UNTESTED
                        },
                        lastTestedTime = obj.optLong("lastTestedTime", 0L),
                        lastResponseTimeMs = obj.optLong("lastResponseTimeMs", 0L),
                        lastError = if (obj.has("lastError")) obj.getString("lastError") else null
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveApiKeys(keys: List<ApiKeyItem>) {
        val array = JSONArray()
        for (item in keys) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("label", item.label)
                put("key", item.key)
                put("isPrimary", item.isPrimary)
                put("isEnabled", item.isEnabled)
                put("status", item.status.name)
                put("lastTestedTime", item.lastTestedTime)
                put("lastResponseTimeMs", item.lastResponseTimeMs)
                item.lastError?.let { put("lastError", it) }
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_API_KEYS_LIST, array.toString()).apply()
    }

    fun getPrimaryApiKey(): ApiKeyItem? {
        val keys = getApiKeys()
        return keys.firstOrNull { it.isPrimary && it.isEnabled } ?: keys.firstOrNull { it.isEnabled }
    }

    fun updateKeyStatus(id: String, status: KeyStatus, responseTimeMs: Long, error: String? = null) {
        val keys = getApiKeys().map { item ->
            if (item.id == id) {
                item.copy(
                    status = status,
                    lastTestedTime = System.currentTimeMillis(),
                    lastResponseTimeMs = responseTimeMs,
                    lastError = error
                )
            } else item
        }
        saveApiKeys(keys)
    }

    fun setPrimaryKey(id: String) {
        val keys = getApiKeys().map { item ->
            item.copy(isPrimary = (item.id == id))
        }
        saveApiKeys(keys)
    }

    fun isAutoFallbackEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_FALLBACK, false)
    }

    fun setAutoFallbackEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_FALLBACK, enabled).apply()
    }

    fun getCustomModels(): List<GeminiModelConfig> {
        val jsonString = prefs.getString(KEY_CUSTOM_MODELS, null) ?: return emptyList()
        val list = mutableListOf<GeminiModelConfig>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    GeminiModelConfig(
                        id = obj.getString("id"),
                        displayName = obj.getString("displayName"),
                        description = obj.optString("description", "Custom user-configured model ID"),
                        isLegacy = obj.optBoolean("isLegacy", false),
                        isCustom = true
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveCustomModels(models: List<GeminiModelConfig>) {
        val array = JSONArray()
        for (item in models) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("displayName", item.displayName)
                put("description", item.description)
                put("isLegacy", item.isLegacy)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_CUSTOM_MODELS, array.toString()).apply()
    }

    fun getSelectedModelId(): String {
        return prefs.getString(KEY_SELECTED_MODEL, "gemini-3.8-flash-tts") ?: "gemini-3.8-flash-tts"
    }

    fun setSelectedModelId(modelId: String) {
        prefs.edit().putString(KEY_SELECTED_MODEL, modelId).apply()
    }

    fun getSelectedVoice(): String {
        return prefs.getString(KEY_SELECTED_VOICE, "Puck") ?: "Puck"
    }

    fun setSelectedVoice(voice: String) {
        prefs.edit().putString(KEY_SELECTED_VOICE, voice).apply()
    }

    fun getSelectedSpeed(): Float {
        return prefs.getFloat(KEY_SELECTED_SPEED, 1.0f)
    }

    fun setSelectedSpeed(speed: Float) {
        prefs.edit().putFloat(KEY_SELECTED_SPEED, speed).apply()
    }

    fun isAutoPlayEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_PLAY, true)
    }

    fun setAutoPlayEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_PLAY, enabled).apply()
    }

    fun getThemeMode(): String {
        return prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM"
    }

    fun setThemeMode(theme: String) {
        prefs.edit().putString(KEY_THEME_MODE, theme).apply()
    }

    companion object {
        private const val KEY_API_KEYS_LIST = "key_api_keys_list"
        private const val KEY_AUTO_FALLBACK = "key_auto_fallback"
        private const val KEY_CUSTOM_MODELS = "key_custom_models"
        private const val KEY_SELECTED_MODEL = "key_selected_model"
        private const val KEY_SELECTED_VOICE = "key_selected_voice"
        private const val KEY_SELECTED_SPEED = "key_selected_speed"
        private const val KEY_AUTO_PLAY = "key_auto_play"
        private const val KEY_THEME_MODE = "key_theme_mode"
    }
}
