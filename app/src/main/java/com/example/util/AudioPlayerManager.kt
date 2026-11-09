package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AudioPlayerManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    data class PlayerState(
        val isPlaying: Boolean = false,
        val isPaused: Boolean = false,
        val currentPositionMs: Int = 0,
        val durationMs: Int = 0,
        val currentFilePath: String? = null,
        val fileName: String? = null,
        val volume: Float = 1.0f,
        val playbackSpeed: Float = 1.0f
    ) {
        val progressFraction: Float
            get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

        val formattedCurrentTime: String
            get() = formatTime(currentPositionMs)

        val formattedDuration: String
            get() = formatTime(durationMs)

        private fun formatTime(millis: Int): String {
            val totalSeconds = millis / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    fun playFile(file: File, playbackSpeed: Float = _playerState.value.playbackSpeed, onCompletion: (() -> Unit)? = null) {
        try {
            stop()
            mediaPlayer?.release()

            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnPreparedListener { mp ->
                    val dur = mp.duration
                    applyPlaybackParams(mp, playbackSpeed)
                    mp.start()
                    _playerState.value = _playerState.value.copy(
                        isPlaying = true,
                        isPaused = false,
                        currentPositionMs = 0,
                        durationMs = dur,
                        currentFilePath = file.absolutePath,
                        fileName = file.name,
                        playbackSpeed = playbackSpeed
                    )
                    startProgressTracking()
                    AppLogger.log("AUDIO", "Started playing: ${file.name} (${dur}ms)")
                }
                setOnCompletionListener {
                    _playerState.value = _playerState.value.copy(
                        isPlaying = false,
                        isPaused = false,
                        currentPositionMs = _playerState.value.durationMs
                    )
                    stopProgressTracking()
                    onCompletion?.invoke()
                    AppLogger.log("AUDIO", "Playback completed for: ${file.name}")
                }
                setOnErrorListener { _, what, extra ->
                    AppLogger.log("AUDIO", "Playback error: what=$what, extra=$extra")
                    stop()
                    false
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            AppLogger.log("AUDIO", "Failed to initialize playback: ${e.message}")
            e.printStackTrace()
        }
    }

    fun pause() {
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.pause()
                _playerState.value = _playerState.value.copy(
                    isPlaying = false,
                    isPaused = true
                )
                stopProgressTracking()
                AppLogger.log("AUDIO", "Playback paused")
            }
        }
    }

    fun resume() {
        mediaPlayer?.let { mp ->
            if (_playerState.value.isPaused) {
                mp.start()
                _playerState.value = _playerState.value.copy(
                    isPlaying = true,
                    isPaused = false
                )
                startProgressTracking()
                AppLogger.log("AUDIO", "Playback resumed")
            }
        }
    }

    fun stop() {
        stopProgressTracking()
        mediaPlayer?.let { mp ->
            try {
                if (mp.isPlaying) {
                    mp.stop()
                }
            } catch (e: Exception) {
                // ignore
            }
        }
        _playerState.value = _playerState.value.copy(
            isPlaying = false,
            isPaused = false,
            currentPositionMs = 0
        )
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.let { mp ->
            val target = positionMs.coerceIn(0, _playerState.value.durationMs)
            mp.seekTo(target)
            _playerState.value = _playerState.value.copy(currentPositionMs = target)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _playerState.value = _playerState.value.copy(playbackSpeed = speed)
        mediaPlayer?.let { mp ->
            applyPlaybackParams(mp, speed)
        }
    }

    private fun applyPlaybackParams(mp: MediaPlayer, speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val params = mp.playbackParams ?: PlaybackParams()
                params.speed = speed
                mp.playbackParams = params
            } catch (e: Exception) {
                AppLogger.log("AUDIO", "Could not set playback speed: ${e.message}")
            }
        }
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _playerState.value = _playerState.value.copy(volume = clamped)
        mediaPlayer?.setVolume(clamped, clamped)
    }

    private fun startProgressTracking() {
        stopProgressTracking()
        progressJob = scope.launch {
            while (isActive) {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        _playerState.value = _playerState.value.copy(
                            currentPositionMs = mp.currentPosition,
                            durationMs = mp.duration
                        )
                    }
                }
                delay(200)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    /**
     * Saves audio to public Downloads / Music directory via MediaStore (Android 10+)
     * or standard external files dir.
     */
    fun saveAudioToDevice(sourceFile: File): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
            val targetFileName = "KRITY_TTS_$timeStamp.wav"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, targetFileName)
                    put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
                    put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/KRITY_TTS")
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        FileInputStream(sourceFile).use { input ->
                            input.copyTo(out)
                        }
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                    AppLogger.log("STORAGE", "Saved audio to Downloads/KRITY_TTS/$targetFileName")
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val appDir = File(downloadsDir, "KRITY_TTS")
                if (!appDir.exists()) appDir.mkdirs()
                val destFile = File(appDir, targetFileName)
                FileInputStream(sourceFile).use { input ->
                    FileOutputStream(destFile).use { out ->
                        input.copyTo(out)
                    }
                }
                AppLogger.log("STORAGE", "Saved audio to ${destFile.absolutePath}")
            }
            sourceFile
        } catch (e: Exception) {
            AppLogger.log("STORAGE", "Failed to save file: ${e.message}")
            null
        }
    }

    fun shareAudio(context: Context, audioFile: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, audioFile)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Share KRITY AI Speech")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            AppLogger.log("SHARE", "Opened share sheet for: ${audioFile.name}")
        } catch (e: Exception) {
            AppLogger.log("SHARE", "Share failed: ${e.message}")
        }
    }
}
