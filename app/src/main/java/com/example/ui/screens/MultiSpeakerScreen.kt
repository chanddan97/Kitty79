package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.StoryScriptSectionEntity
import com.example.data.local.StorySpeakerEntity
import com.example.data.model.DefaultModelPresets
import com.example.ui.theme.AccentError
import com.example.ui.theme.AccentSuccess
import com.example.ui.theme.AccentWarning
import com.example.viewmodel.TtsViewModel
import java.io.File
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R

@Composable
fun MultiSpeakerScreen(
    viewModel: TtsViewModel,
    modifier: Modifier = Modifier
) {
    val activeProjectId by viewModel.activeProjectId.collectAsState()
    val allProjects by viewModel.allProjects.collectAsState()
    val speakers by viewModel.activeProjectSpeakers.collectAsState()
    val scriptSections by viewModel.activeProjectScriptSections.collectAsState()
    val isAnalyzing by viewModel.isAnalyzingStory.collectAsState()
    val isGenerating by viewModel.isGeneratingChapters.collectAsState()
    val generationProgress by viewModel.multiSpeakerGenerationProgress.collectAsState()
    val combinedAudio by viewModel.combinedAudioResult.collectAsState()
    val playerState by viewModel.playerState.collectAsState()

    var storyText by remember { mutableStateOf("") }
    var projectTitle by remember { mutableStateOf("") }

    val activeProject = allProjects.firstOrNull { it.id == activeProjectId && it.projectType == "MULTI_SPEAKER" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "STORY STUDIO",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Multi-speaker character voice production",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (activeProject == null) {
            // Hero Banner for Story Studio
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_story_banner_1791138912866),
                    contentDescription = "Story Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                )
            }

            // Input Mode
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = projectTitle,
                        onValueChange = { projectTitle = it },
                        label = { Text("Production Title") },
                        placeholder = { Text("e.g. দ্য অ্যাডভেঞ্চার অফ নীলকণ্ঠ") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = storyText,
                        onValueChange = { storyText = it },
                        label = { Text("Paste Story / Script") },
                        placeholder = { Text("আজ আর স্কুলে যাবো না।\n\nরাহুল বলল, \"তুই কোথায় যাচ্ছিস?\"\n...") },
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = { viewModel.createMultiSpeakerProject(projectTitle, storyText) { } },
                        enabled = storyText.isNotBlank() && !isAnalyzing,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("ANALYZING STORY...")
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ANALYZE & DETECT SPEAKERS")
                        }
                    }
                }
            }
        } else {
            // Production Mode
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                item {
                    ProductionStatusCard(
                        projectTitle = activeProject.title,
                        isGenerating = isGenerating,
                        progress = generationProgress,
                        onGenerate = { viewModel.startMultiSpeakerGeneration(activeProject.id) },
                        onPause = { viewModel.pauseChapterQueue() }
                    )
                }

                item {
                    Text("DETECTED SPEAKERS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }

                items(speakers, key = { it.id }) { speaker ->
                    SpeakerVoiceCard(
                        speaker = speaker,
                        onVoiceSelected = { viewModel.updateSpeakerVoice(speaker.id, it) }
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("STORY SCRIPT", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { viewModel.selectProject(-1) }) { // Reset to new
                             Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                             Spacer(modifier = Modifier.width(4.dp))
                             Text("NEW PRODUCTION", fontSize = 11.sp)
                        }
                    }
                }

                items(scriptSections, key = { it.id }) { section ->
                    ScriptSectionItem(section = section)
                }
            }
        }

        // Global Player for Combined Audio
        combinedAudio?.let { file ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { 
                        if (playerState.isPlaying) viewModel.playerManager.pause()
                        else viewModel.playerManager.playFile(file)
                    }) {
                        Icon(
                            if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Continuous Story Audio Ready", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Tap to play the full assembled production", style = MaterialTheme.typography.labelSmall)
                    }
                    Button(onClick = { viewModel.playerManager.shareAudio(viewModel.getApplication(), file) }) {
                        Text("SHARE", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ProductionStatusCard(
    projectTitle: String,
    isGenerating: Boolean,
    progress: Float,
    onGenerate: () -> Unit,
    onPause: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(projectTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            
            if (isGenerating) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Generating production...", style = MaterialTheme.typography.labelSmall)
                    Text("${(progress * 100).toInt()}%", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onPause, colors = ButtonDefaults.buttonColors(containerColor = AccentWarning), modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Pause, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PAUSE PRODUCTION")
                }
            } else {
                Button(onClick = onGenerate, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GENERATE COMPLETE STORY")
                }
            }
        }
    }
}

@Composable
fun SpeakerVoiceCard(
    speaker: StorySpeakerEntity,
    onVoiceSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    OutlinedCard(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(speaker.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Role: ${speaker.speakerType} • Age: ${speaker.age}", style = MaterialTheme.typography.labelSmall)
            }

            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(speaker.voiceName, fontSize = 11.sp)
                }
                
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DefaultModelPresets.voices.forEach { voice ->
                        DropdownMenuItem(
                            text = { Text("${voice.name} (${voice.gender})") },
                            onClick = {
                                onVoiceSelected(voice.name)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScriptSectionItem(section: StoryScriptSectionEntity) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(section.speakerName, fontWeight = FontWeight.Black, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                if (section.status == "COMPLETED") {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentSuccess, modifier = Modifier.size(14.dp))
                } else if (section.status == "GENERATING") {
                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(section.text, style = MaterialTheme.typography.bodySmall, lineHeight = 18.sp)
        }
    }
}
