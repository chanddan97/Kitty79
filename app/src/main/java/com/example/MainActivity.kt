package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.ApiKeyManagerDialog
import com.example.ui.components.ChunkingPromptDialog
import com.example.ui.components.LogViewerDialog
import com.example.ui.components.ModelSelectorDialog
import com.example.ui.screens.ChaptersScreen
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MultiSpeakerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.UsageScreen
import com.example.ui.theme.KrityTtsTheme
import com.example.util.AppLogger
import com.example.viewmodel.TtsViewModel

enum class AppScreen(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    HOME("Studio", Icons.Default.Mic),
    STORY("Story AI", Icons.Default.AutoAwesome),
    CHAPTERS("Chapters", Icons.Default.List),
    HISTORY("History", Icons.Default.History),
    USAGE("Usage", Icons.Default.BarChart),
    DIAGNOSTICS("Health", Icons.Default.HealthAndSafety),
    SETTINGS("Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: TtsViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()

            KrityTtsTheme(themeMode = themeMode) {
                MainApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainApp(viewModel: TtsViewModel) {
    var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
    var showKeyManagerDialog by remember { mutableStateOf(false) }
    var showModelSelectorDialog by remember { mutableStateOf(false) }
    var showLogViewerDialog by remember { mutableStateOf(false) }

    val apiKeys by viewModel.apiKeys.collectAsState()
    val autoFallback by viewModel.autoFallback.collectAsState()
    val isTestingKey by viewModel.isTestingKey.collectAsState()
    val testKeyResult by viewModel.testKeyResult.collectAsState()
    val showChunkDialog by viewModel.showChunkDialog.collectAsState()
    val textAnalysis by viewModel.textAnalysis.collectAsState()
    val allModels by viewModel.allModels.collectAsState()
    val selectedModelId by viewModel.selectedModelId.collectAsState()
    val logs by viewModel.logsFlow.collectAsState()

    var showWelcomeDialog by remember {
        mutableStateOf(apiKeys.isEmpty())
    }

    // Handle back button: if not on Home, navigate back to Home
    BackHandler(enabled = currentScreen != AppScreen.HOME) {
        currentScreen = AppScreen.HOME
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(modifier = Modifier.testTag("bottom_navigation_bar")) {
                        AppScreen.values().forEach { screen ->
                            NavigationBarItem(
                                selected = currentScreen == screen,
                                onClick = { currentScreen = screen },
                                icon = { Icon(screen.icon, contentDescription = screen.title) },
                                label = { Text(screen.title) },
                                modifier = Modifier.testTag("nav_tab_${screen.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                if (isWideScreen) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        AppScreen.values().forEach { screen ->
                            NavigationRailItem(
                                selected = currentScreen == screen,
                                onClick = { currentScreen = screen },
                                icon = { Icon(screen.icon, contentDescription = screen.title) },
                                label = { Text(screen.title) },
                                modifier = Modifier.testTag("rail_tab_${screen.name.lowercase()}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (currentScreen) {
                        AppScreen.HOME -> HomeScreen(
                            viewModel = viewModel,
                            onOpenKeyManager = { showKeyManagerDialog = true },
                            onOpenModelSelector = { showModelSelectorDialog = true },
                            onOpenDiagnostics = { currentScreen = AppScreen.DIAGNOSTICS },
                            onNavigateToStory = { currentScreen = AppScreen.STORY }
                        )
                        AppScreen.STORY -> com.example.ui.screens.MultiSpeakerScreen(viewModel = viewModel)
                        AppScreen.CHAPTERS -> ChaptersScreen(viewModel = viewModel)
                        AppScreen.HISTORY -> HistoryScreen(
                            viewModel = viewModel,
                            onNavigateToHome = { currentScreen = AppScreen.HOME }
                        )
                        AppScreen.USAGE -> UsageScreen(viewModel = viewModel)
                        AppScreen.DIAGNOSTICS -> DiagnosticsScreen(viewModel = viewModel)
                        AppScreen.SETTINGS -> SettingsScreen(
                            viewModel = viewModel,
                            onOpenKeyManager = { showKeyManagerDialog = true },
                            onOpenModelSelector = { showModelSelectorDialog = true },
                            onOpenLogViewer = { showLogViewerDialog = true },
                            onOpenDiagnostics = { currentScreen = AppScreen.DIAGNOSTICS }
                        )
                    }
                }
            }
        }
    }

    // API Key Manager Dialog
    if (showKeyManagerDialog) {
        ApiKeyManagerDialog(
            keys = apiKeys,
            autoFallback = autoFallback,
            isTestingKey = isTestingKey,
            testResult = testKeyResult,
            onAddKey = { label, key -> viewModel.addApiKey(label, key) },
            onUpdateKey = { id, label, key -> viewModel.updateApiKey(id, label, key) },
            onDeleteKey = { id -> viewModel.deleteApiKey(id) },
            onSetPrimary = { id -> viewModel.setPrimaryKey(id) },
            onToggleAutoFallback = { viewModel.toggleAutoFallback(it) },
            onTestKey = { keyItem -> viewModel.testSingleKey(keyItem) },
            onDismissTestResult = { viewModel.dismissTestKeyResult() },
            onDismiss = { showKeyManagerDialog = false }
        )
    }

    // Model Selector Dialog
    if (showModelSelectorDialog) {
        ModelSelectorDialog(
            models = allModels,
            selectedModelId = selectedModelId,
            onSelectModel = {
                viewModel.selectModel(it)
                showModelSelectorDialog = false
            },
            onAddCustomModel = { id, name, desc ->
                viewModel.addCustomModel(id, name, desc)
                showModelSelectorDialog = false
            },
            onDismiss = { showModelSelectorDialog = false }
        )
    }

    // Log Viewer Dialog
    if (showLogViewerDialog) {
        LogViewerDialog(
            logs = logs,
            onClear = { AppLogger.clear() },
            onDismiss = { showLogViewerDialog = false }
        )
    }

    // Chunking Prompt Dialog
    if (showChunkDialog) {
        ChunkingPromptDialog(
            charCount = textAnalysis.charCount,
            onProceedWithSplit = { viewModel.onProceedWithChunking(split = true) },
            onProceedWithoutSplit = { viewModel.onProceedWithChunking(split = false) },
            onDismiss = { viewModel.onDismissChunkDialog() }
        )
    }

    // First Launch Welcome Experience (Requirement 49)
    if (showWelcomeDialog && apiKeys.isEmpty()) {
        AlertDialog(
            onDismissRequest = { showWelcomeDialog = false },
            title = {
                Row {
                    Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(" Welcome to KRITY AI TTS", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text("Enter your Gemini API key to begin natural AI speech generation for Bengali, Hindi, and English. You can test your key before generating audio.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showWelcomeDialog = false
                        showKeyManagerDialog = true
                    }
                ) {
                    Text("ADD API KEY")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWelcomeDialog = false }) {
                    Text("CONTINUE")
                }
            }
        )
    }
}
