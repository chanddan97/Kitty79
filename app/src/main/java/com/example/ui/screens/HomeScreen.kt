package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DefaultModelPresets
import com.example.data.model.SpeakingStyle
import com.example.data.model.TtsLanguage
import com.example.data.model.TtsScene
import com.example.data.model.VoiceEmotion
import com.example.data.model.VoiceOption
import com.example.ui.components.AudioPlayerCard
import com.example.ui.theme.AccentError
import com.example.ui.theme.AccentSuccess
import com.example.ui.theme.AccentWarning
import com.example.viewmodel.TtsViewModel
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R

@Composable
fun HomeScreen(
    viewModel: TtsViewModel,
    onOpenKeyManager: () -> Unit,
    onOpenModelSelector: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onNavigateToStory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val textInput by viewModel.textInput.collectAsState()
    val textAnalysis by viewModel.textAnalysis.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val selectedModelId by viewModel.selectedModelId.collectAsState()
    val allModels by viewModel.allModels.collectAsState()
    val selectedScene by viewModel.selectedScene.collectAsState()
    val customSceneText by viewModel.customSceneText.collectAsState()
    val selectedVoiceName by viewModel.selectedVoiceName.collectAsState()
    val selectedStyle by viewModel.selectedStyle.collectAsState()
    val selectedEmotion by viewModel.selectedEmotion.collectAsState()
    val customEmotionText by viewModel.customEmotionText.collectAsState()
    val emotionIntensity by viewModel.emotionIntensity.collectAsState()
    val customInstruction by viewModel.customInstruction.collectAsState()
    val selectedSpeed by viewModel.selectedSpeed.collectAsState()
    val genStep by viewModel.genStep.collectAsState()
    val progressMessage by viewModel.progressMessage.collectAsState()
    val lastGeneratedResult by viewModel.lastGeneratedResult.collectAsState()
    val generationError by viewModel.generationError.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val apiKeys by viewModel.apiKeys.collectAsState()

    var showCustomPromptExpanded by remember { mutableStateOf(false) }

    val activeModel = allModels.firstOrNull { it.id == selectedModelId }
        ?: DefaultModelPresets.models.first()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // App Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "KRITY AI TTS",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Studio",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(onClick = onOpenKeyManager) {
                Icon(
                    Icons.Default.Key, 
                    contentDescription = "Keys", 
                    tint = if (apiKeys.any { it.isEnabled }) AccentSuccess else AccentWarning
                )
            }
        }

        // Hero Banner
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_home_banner_1791138928820),
                contentDescription = "Home Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            )
        }

        // Error Banner if present
        generationError?.let { err ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = AccentError, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generation Error", fontWeight = FontWeight.Bold, color = AccentError, style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.dismissError()
                                viewModel.onGenerateClicked()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentError),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("RETRY", fontSize = 11.sp)
                        }
                        TextButton(
                            onClick = { viewModel.dismissError() },
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("DISMISS", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Card 1: Text Input Studio
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TEXT STUDIO",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row {
                        TextButton(
                            onClick = {
                                // Smart Cleanup Logic
                                val cleaned = textInput.replace(Regex("^.*?(:| say(s)?| said):?", RegexOption.MULTILINE), "").trim()
                                viewModel.onTextChanged(cleaned)
                            },
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("CLEANUP", fontSize = 11.sp)
                        }
                        
                        TextButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = clipboard.primaryClip
                                if (clip != null && clip.itemCount > 0) {
                                    val pasted = clip.getItemAt(0).text?.toString() ?: ""
                                    viewModel.onTextChanged(textInput + (if (textInput.isNotEmpty()) " " else "") + pasted)
                                }
                            },
                            modifier = Modifier.height(30.dp).testTag("paste_button")
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PASTE", fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = textInput,
                    onValueChange = { viewModel.onTextChanged(it) },
                    placeholder = {
                        Text(
                            text = "Write or paste text here...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 220.dp)
                        .testTag("text_input_field"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Stats and Language Segment
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "${textAnalysis.charCount} chars",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = "~${textAnalysis.formattedEstimatedDuration}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Compact Language Selector Dropdown
                    var langMenuExpanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { langMenuExpanded = true },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                text = if (selectedLanguage == TtsLanguage.AUTO) "🌐 Auto" else selectedLanguage.displayName,
                                fontSize = 11.sp
                            )
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                        DropdownMenu(
                            expanded = langMenuExpanded,
                            onDismissRequest = { langMenuExpanded = false }
                        ) {
                            TtsLanguage.values().forEach { lang ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (lang == TtsLanguage.AUTO) "Auto Detect" else lang.displayName,
                                            fontWeight = if (selectedLanguage == lang) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        viewModel.selectLanguage(lang)
                                        langMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Story Studio Entry Card
        Card(
            onClick = onNavigateToStory,
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().testTag("go_to_story_studio_card")
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.secondary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("AI Story Studio", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text("Create multi-speaker character audio productions", style = MaterialTheme.typography.labelSmall)
                }
                Icon(Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.rotate(-90f))
            }
        }

        // Card 2: Voice & TTS Model (Modern Dropdowns)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "VOICE & MODEL SETUP",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Voice Dropdown
                val currentVoice = DefaultModelPresets.voices.firstOrNull { it.name == selectedVoiceName }
                    ?: DefaultModelPresets.voices.first()

                ModernDropdownField(
                    label = "AI Voice Character",
                    selectedValueText = "${currentVoice.name} (${currentVoice.gender})",
                    selectedSubtitle = currentVoice.description,
                    leadingIcon = Icons.Default.RecordVoiceOver,
                    items = DefaultModelPresets.voices,
                    onItemSelected = { viewModel.selectVoice(it.name) },
                    itemLabel = { "${it.name} (${it.gender})" },
                    itemSubtitle = { it.description },
                    isSelected = { it.name == selectedVoiceName },
                    extraTrailingAction = {
                        IconButton(
                            onClick = { viewModel.previewVoice(currentVoice) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Preview voice",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )

                // Model Dropdown
                ModernDropdownField(
                    label = "TTS Engine Model",
                    selectedValueText = activeModel.displayName,
                    selectedSubtitle = if (activeModel.isLegacy) "Legacy model" else activeModel.description,
                    leadingIcon = Icons.Default.Tune,
                    items = allModels,
                    onItemSelected = { viewModel.selectModel(it.id) },
                    itemLabel = { it.displayName },
                    itemSubtitle = { it.description },
                    isSelected = { it.id == selectedModelId }
                )
            }
        }

        // Card 3: Scene & Context (Modern Dropdown + Manual Scene Input Box)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SCENE & CONTEXT (দৃশ্যপট ও উদ্দেশ্য)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Tells Gemini AI why & how to speak",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Scene Modern Dropdown
                ModernDropdownField(
                    label = "Scene Preset",
                    selectedValueText = "${selectedScene.nameEn} (${selectedScene.nameBn})",
                    selectedSubtitle = selectedScene.description,
                    leadingIcon = Icons.Default.Movie,
                    items = viewModel.availableScenes,
                    onItemSelected = { viewModel.selectScene(it) },
                    itemLabel = { "${it.nameEn} (${it.nameBn})" },
                    itemSubtitle = { it.description },
                    isSelected = { it.id == selectedScene.id }
                )

                // Manual Scene Context Input Box
                OutlinedTextField(
                    value = customSceneText,
                    onValueChange = { viewModel.setCustomSceneText(it) },
                    label = { Text("Manual Scene Context (কাস্টম প্রেক্ষাপট)") },
                    placeholder = {
                        Text(
                            "যেমন: গা ছমছমে হরর গল্প, ক্রিকেট ধারাভাষ্য, কাস্টমার কেয়ার...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (customSceneText.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setCustomSceneText("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_scene_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    singleLine = true
                )
            }
        }

        // Card 4: Style, Emotion & Nuance (Modern Dropdown + Manual Emotion Box)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "STYLE & EMOTIONAL EXPRESSION",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Speaking Style Dropdown
                ModernDropdownField(
                    label = "Speaking Style",
                    selectedValueText = selectedStyle.displayName,
                    selectedSubtitle = selectedStyle.instruction,
                    leadingIcon = Icons.Default.Mood,
                    items = SpeakingStyle.values().toList(),
                    onItemSelected = { viewModel.selectStyle(it) },
                    itemLabel = { it.displayName },
                    itemSubtitle = { it.instruction },
                    isSelected = { it == selectedStyle }
                )

                // Emotion Dropdown
                ModernDropdownField(
                    label = "Preset Emotion Baseline",
                    selectedValueText = selectedEmotion.displayName,
                    selectedSubtitle = selectedEmotion.promptDescriptor,
                    leadingIcon = Icons.Default.Mood,
                    items = VoiceEmotion.values().toList(),
                    onItemSelected = { viewModel.selectEmotion(it) },
                    itemLabel = { it.displayName },
                    itemSubtitle = { it.promptDescriptor },
                    isSelected = { it == selectedEmotion }
                )

                // Manual Emotion Nuance Input Box
                OutlinedTextField(
                    value = customEmotionText,
                    onValueChange = { viewModel.setCustomEmotionText(it) },
                    label = { Text("Custom Emotion Nuance (আবেগ ও সুরের নিজস্ব বিবরণ)") },
                    placeholder = {
                        Text(
                            "যেমন: কান্নামাখা স্বর, মৃদু চাপা হাসি, চরম ক্রোধ, অভিমানী সুর...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (customEmotionText.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setCustomEmotionText("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_emotion_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    singleLine = true
                )

                // Emotion Intensity Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Emotion Intensity",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${(emotionIntensity * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = emotionIntensity,
                        onValueChange = { viewModel.setEmotionIntensity(it) },
                        valueRange = 0.1f..1.0f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("emotion_slider"),
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }

        // Card 5: Speed Selector
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SPEAKING SPEED",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(0.75f, 1.0f, 1.25f, 1.5f).forEach { spd ->
                        val isSelected = kotlin.math.abs(selectedSpeed - spd) < 0.04f
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectSpeed(spd) },
                            label = { Text("${spd}x", fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            modifier = Modifier.testTag("speed_chip_${spd}")
                        )
                    }
                }
            }
        }

        // Collapsible Advanced Prompt Guidance
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCustomPromptExpanded = !showCustomPromptExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Additional Voice Guidance (Optional)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = if (showCustomPromptExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }

                AnimatedVisibility(visible = showCustomPromptExpanded) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        OutlinedTextField(
                            value = customInstruction,
                            onValueChange = { viewModel.setCustomInstruction(it) },
                            placeholder = { Text("e.g. Pause slightly after commas, emphasize key nouns...", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            maxLines = 2
                        )
                    }
                }
            }
        }

        // PRIMARY ACTION: GENERATE VOICE BUTTON
        val isGenerating = genStep in listOf(
            TtsViewModel.GenStep.PREPARING,
            TtsViewModel.GenStep.CONNECTING,
            TtsViewModel.GenStep.GENERATING,
            TtsViewModel.GenStep.PROCESSING
        )

        Button(
            onClick = { viewModel.onGenerateClicked() },
            enabled = !isGenerating,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("generate_voice_button")
        ) {
            if (isGenerating) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = progressMessage.ifEmpty { "Generating Voice..." },
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            } else {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "GENERATE AI VOICE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Audio Player Card
        lastGeneratedResult?.let { _ ->
            AudioPlayerCard(
                playerState = playerState,
                onPlay = { viewModel.playCurrentResult() },
                onPause = { viewModel.playerManager.pause() },
                onStop = { viewModel.playerManager.stop() },
                onSeek = { viewModel.playerManager.seekTo(it) },
                onVolumeChange = { viewModel.playerManager.setVolume(it) },
                onSpeedChange = { viewModel.selectSpeed(it) },
                onSaveWav = { viewModel.saveCurrentResult(context) },
                onShare = { viewModel.shareCurrentResult(context) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Long Text Chunking Dialog
    if (viewModel.showChunkDialog.collectAsState().value) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissChunkDialog() },
            title = { Text("Long Text Detected") },
            text = {
                Text(
                    "Your text contains ${textInput.length} characters. Would you like to split it automatically into sentence chunks to preserve optimal acoustic quality?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.onProceedWithChunking(split = true) }) {
                    Text("SPLIT & GENERATE")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onProceedWithChunking(split = false) }) {
                    Text("GENERATE AS SINGLE")
                }
            }
        )
    }

    // Processed Text Preview Dialog (Feature 1 PRD)
    val showProcessedDialog by viewModel.showProcessedTextPreview.collectAsState()
    val smartResult by viewModel.smartProcessedText.collectAsState()
    if (showProcessedDialog) {
        com.example.ui.components.ProcessedTextDialog(
            processedResult = smartResult,
            onDismiss = { viewModel.dismissProcessedPreview() }
        )
    }
}

/**
 * Reusable Modern Material 3 Dropdown Field
 */
@Composable
fun <T> ModernDropdownField(
    label: String,
    selectedValueText: String,
    selectedSubtitle: String? = null,
    leadingIcon: ImageVector? = null,
    items: List<T>,
    onItemSelected: (T) -> Unit,
    itemLabel: (T) -> String,
    itemSubtitle: ((T) -> String)? = null,
    isSelected: (T) -> Boolean,
    modifier: Modifier = Modifier,
    extraTrailingAction: (@Composable () -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "arrow_rot")

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedCard(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { expanded = !expanded }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (leadingIcon != null) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = leadingIcon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                        }

                        Column {
                            Text(
                                text = selectedValueText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (!selectedSubtitle.isNullOrBlank()) {
                                Text(
                                    text = selectedSubtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        extraTrailingAction?.invoke()
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(rotation)
                        )
                    }
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .heightIn(max = 320.dp)
            ) {
                items.forEach { item ->
                    val selected = isSelected(item)
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = itemLabel(item),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                itemSubtitle?.let { sub ->
                                    val subText = sub(item)
                                    if (subText.isNotBlank()) {
                                        Text(
                                            text = subText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        },
                        trailingIcon = if (selected) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else null,
                        onClick = {
                            onItemSelected(item)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
