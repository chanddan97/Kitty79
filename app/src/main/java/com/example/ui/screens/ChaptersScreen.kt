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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ChapterEntity
import com.example.data.local.ProjectEntity
import com.example.ui.theme.AccentError
import com.example.ui.theme.AccentSuccess
import com.example.ui.theme.AccentWarning
import com.example.viewmodel.TtsViewModel

@Composable
fun ChaptersScreen(
    viewModel: TtsViewModel,
    modifier: Modifier = Modifier
) {
    val allProjects by viewModel.allProjects.collectAsState()
    val activeProjectId by viewModel.activeProjectId.collectAsState()
    val activeChapters by viewModel.activeProjectChapters.collectAsState()
    val isGenerating by viewModel.isGeneratingChapters.collectAsState()
    val isPaused by viewModel.isChapterPaused.collectAsState()
    val activeGeneratingId by viewModel.activeGeneratingChapterId.collectAsState()
    val unfinishedResume by viewModel.unfinishedProjectToResume.collectAsState()
    val textInput by viewModel.textInput.collectAsState()

    var showNewProjectDialog by remember { mutableStateOf(false) }
    var renameProjectTarget by remember { mutableStateOf<ProjectEntity?>(null) }

    val activeProject = allProjects.firstOrNull { it.id == activeProjectId }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CHAPTER & BOOK STUDIO",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Smart splitting & sequential queue generator",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { showNewProjectDialog = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("new_project_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("NEW PROJECT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Active Project Queue Card
        if (activeProject != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeProject.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Model: ${activeProject.modelId} • Voice: ${activeProject.voiceName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Project Progress Badge
                        val progressPercent = if (activeProject.totalChapters > 0) {
                            (activeProject.completedChapters.toFloat() / activeProject.totalChapters.toFloat())
                        } else 0f

                        Text(
                            text = "${activeProject.completedChapters}/${activeProject.totalChapters} Parts",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val progressFloat = if (activeProject.totalChapters > 0) {
                        activeProject.completedChapters.toFloat() / activeProject.totalChapters.toFloat()
                    } else 0f
                    LinearProgressIndicator(
                        progress = { progressFloat },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Queue Controls: Generate All, Pause, Resume, Cancel
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!isGenerating) {
                            Button(
                                onClick = { viewModel.startChapterQueue(activeProject.id) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (activeProject.completedChapters > 0) "RESUME ALL" else "GENERATE ALL", fontSize = 11.sp)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.pauseChapterQueue() },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentWarning),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("PAUSE", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = { viewModel.cancelChapterQueue(activeProject.id) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("CANCEL", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Chapters Queue List
        if (activeChapters.isNotEmpty()) {
            Text(
                text = "GENERATION QUEUE (${activeChapters.size} PARTS)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(activeChapters, key = { it.id }) { chapter ->
                    val isThisGenerating = activeGeneratingId == chapter.id
                    ChapterItemCard(
                        chapter = chapter,
                        isCurrentlyGenerating = isThisGenerating,
                        onPlay = { viewModel.playChapterAudio(chapter) },
                        onRetry = { viewModel.retrySingleChapter(chapter.id) }
                    )
                }
            }
        } else {
            // Projects Browser
            Text(
                text = "SAVED PROJECTS (${allProjects.size})",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (allProjects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No Long Text Projects Yet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Paste an entire book, story, or long script.\nKRITY AI will intelligently split and queue it into parts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { showNewProjectDialog = true }) {
                            Text("CREATE FIRST PROJECT")
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    items(allProjects, key = { it.id }) { proj ->
                        OutlinedCard(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectProject(proj.id) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(proj.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "${proj.completedChapters}/${proj.totalChapters} Parts Completed • Status: ${proj.status}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row {
                                    IconButton(onClick = { renameProjectTarget = proj }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Rename", modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(onClick = { viewModel.deleteProject(proj.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentError, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // New Project Dialog
    if (showNewProjectDialog) {
        var projectTitle by remember { mutableStateOf("") }
        var projectText by remember { mutableStateOf(textInput) }

        AlertDialog(
            onDismissRequest = { showNewProjectDialog = false },
            title = { Text("New Chapter Project") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = projectTitle,
                        onValueChange = { projectTitle = it },
                        label = { Text("Project Title") },
                        placeholder = { Text("e.g. রূপকথার গল্প - পর্ব ০১") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = projectText,
                        onValueChange = { projectText = it },
                        label = { Text("Book or Story Text") },
                        placeholder = { Text("Paste entire story or document...") },
                        modifier = Modifier.fillMaxWidth().height(160.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (projectText.isNotBlank()) {
                            viewModel.createProjectFromText(projectTitle, projectText) {
                                showNewProjectDialog = false
                            }
                        }
                    }
                ) {
                    Text("SPLIT & CREATE")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewProjectDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    // Resume Unfinished Generation Dialog (Feature 6 PRD)
    unfinishedResume?.let { proj ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissResumePrompt() },
            title = { Text("Unfinished Generation Found") },
            text = {
                Text(
                    "Project '${proj.title}' was interrupted at part ${proj.completedChapters + 1} of ${proj.totalChapters}. Would you like to resume from where you left off?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.selectProject(proj.id)
                        viewModel.dismissResumePrompt()
                        viewModel.startChapterQueue(proj.id)
                    }
                ) {
                    Text("RESUME QUEUE")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.startOverProject(proj.id)
                    }
                ) {
                    Text("START OVER")
                }
            }
        )
    }

    // Rename Dialog
    renameProjectTarget?.let { proj ->
        var editTitle by remember { mutableStateOf(proj.title) }
        AlertDialog(
            onDismissRequest = { renameProjectTarget = null },
            title = { Text("Rename Project") },
            text = {
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    label = { Text("Project Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameProject(proj.id, editTitle)
                        renameProjectTarget = null
                    }
                ) {
                    Text("SAVE")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameProjectTarget = null }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

@Composable
fun ChapterItemCard(
    chapter: ChapterEntity,
    isCurrentlyGenerating: Boolean,
    onPlay: () -> Unit,
    onRetry: () -> Unit
) {
    OutlinedCard(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isCurrentlyGenerating) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Icon Indicator
                when {
                    isCurrentlyGenerating -> {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                    chapter.status == "COMPLETED" -> {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Complete", tint = AccentSuccess, modifier = Modifier.size(20.dp))
                    }
                    chapter.status == "FAILED" -> {
                        Icon(Icons.Default.Error, contentDescription = "Failed", tint = AccentError, modifier = Modifier.size(20.dp))
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${chapter.chapterIndex}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(chapter.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.width(6.dp))
                        val statusLabel = when {
                            isCurrentlyGenerating -> "Generating..."
                            chapter.status == "COMPLETED" -> "Complete"
                            chapter.status == "FAILED" -> "Failed"
                            else -> "Waiting"
                        }
                        val statusColor = when {
                            isCurrentlyGenerating -> MaterialTheme.colorScheme.primary
                            chapter.status == "COMPLETED" -> AccentSuccess
                            chapter.status == "FAILED" -> AccentError
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = chapter.content.take(65).replace("\n", " ") + "...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            // Action Button
            Row {
                if (chapter.status == "COMPLETED") {
                    IconButton(onClick = onPlay, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = MaterialTheme.colorScheme.primary)
                    }
                } else if (chapter.status == "FAILED") {
                    IconButton(onClick = onRetry, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "Retry part", tint = AccentError)
                    }
                }
            }
        }
    }
}
