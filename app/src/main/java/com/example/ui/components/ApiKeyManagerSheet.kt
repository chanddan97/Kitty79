package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.GeminiTtsService
import com.example.data.model.ApiKeyItem
import com.example.data.model.KeyStatus
import com.example.ui.theme.AccentError
import com.example.ui.theme.AccentSuccess
import com.example.ui.theme.AccentWarning

@Composable
fun ApiKeyManagerDialog(
    keys: List<ApiKeyItem>,
    autoFallback: Boolean,
    isTestingKey: Boolean,
    testResult: GeminiTtsService.ApiTestResult?,
    onAddKey: (String, String) -> Unit,
    onUpdateKey: (String, String, String) -> Unit,
    onDeleteKey: (String) -> Unit,
    onSetPrimary: (String) -> Unit,
    onToggleAutoFallback: (Boolean) -> Unit,
    onTestKey: (ApiKeyItem) -> Unit,
    onDismissTestResult: () -> Unit,
    onDismiss: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingKey by remember { mutableStateOf<ApiKeyItem?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Key, contentDescription = "Keys", tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("API KEY MANAGER", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Auto Fallback Row
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto Key Fallback", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Switch to backup key automatically on rate limit (429)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoFallback,
                            onCheckedChange = onToggleAutoFallback,
                            modifier = Modifier.testTag("auto_fallback_switch")
                        )
                    }
                }

                if (isTestingKey) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Auto-testing API key with Gemini TTS...",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Security Note
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, contentDescription = "Security", tint = AccentWarning, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Keys are stored securely in local app storage. Complete keys are never exposed in logs.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // List of Keys
                if (keys.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No API Keys configured.\nAdd a Gemini API key to start.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(keys, key = { it.id }) { item ->
                            ApiKeyCard(
                                item = item,
                                isTesting = isTestingKey,
                                onTest = { onTestKey(item) },
                                onSetPrimary = { onSetPrimary(item.id) },
                                onEdit = { editingKey = item },
                                onDelete = { onDeleteKey(item.id) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth().testTag("add_key_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ADD API KEY")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("DONE")
            }
        }
    )

    // Add Key Dialog
    if (showAddDialog) {
        AddEditKeyDialog(
            initialLabel = "Key #${keys.size + 1}",
            initialKey = "",
            isEdit = false,
            onConfirm = { label, key ->
                onAddKey(label, key)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    // Edit Key Dialog
    editingKey?.let { item ->
        AddEditKeyDialog(
            initialLabel = item.label,
            initialKey = item.key,
            isEdit = true,
            onConfirm = { label, key ->
                onUpdateKey(item.id, label, key)
                editingKey = null
            },
            onDismiss = { editingKey = null }
        )
    }

    // Live Test Result Dialog
    testResult?.let { result ->
        ApiTestResultDialog(result = result, onDismiss = onDismissTestResult)
    }
}

@Composable
fun ApiKeyCard(
    item: ApiKeyItem,
    isTesting: Boolean,
    onTest: () -> Unit,
    onSetPrimary: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (item.isPrimary) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (item.isPrimary) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PRIMARY",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentError, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Masked Key
            Text(
                text = item.maskedKey,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val (statusColor, statusText) = when (item.status) {
                    KeyStatus.WORKING -> AccentSuccess to "● Working (${item.lastResponseTimeMs}ms)"
                    KeyStatus.RATE_LIMITED -> AccentWarning to "● Rate Limited (429)"
                    KeyStatus.INVALID -> AccentError to "● Invalid / Expired"
                    KeyStatus.ERROR -> AccentError to "● Error"
                    KeyStatus.UNTESTED -> MaterialTheme.colorScheme.onSurfaceVariant to "● Not Tested"
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Medium
                )

                Row {
                    if (!item.isPrimary) {
                        TextButton(onClick = onSetPrimary, modifier = Modifier.height(28.dp)) {
                            Text("SET PRIMARY", fontSize = 11.sp)
                        }
                    }
                    OutlinedButton(
                        onClick = onTest,
                        enabled = !isTesting,
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("TEST", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditKeyDialog(
    initialLabel: String,
    initialKey: String,
    isEdit: Boolean,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf(initialLabel) }
    var key by remember { mutableStateOf(initialKey) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Edit API Key" else "Add Gemini API Key") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label (e.g. Personal Key, Backup Key)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = key,
                    onValueChange = {
                        key = it
                        errorText = null
                    },
                    label = { Text("Gemini API Key (AIzaSy...)") },
                    singleLine = true,
                    isError = errorText != null,
                    supportingText = errorText?.let { { Text(it, color = AccentError) } },
                    modifier = Modifier.fillMaxWidth().testTag("api_key_input")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚡ Auto-Test: The key will be tested live automatically upon saving and the diagnostic results will be displayed immediately.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (key.trim().isBlank()) {
                        errorText = "API key cannot be empty"
                    } else {
                        onConfirm(label, key)
                    }
                }
            ) {
                Text(if (isEdit) "UPDATE" else "SAVE KEY")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}

@Composable
fun ApiTestResultDialog(
    result: GeminiTtsService.ApiTestResult,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (result.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (result.isSuccess) AccentSuccess else AccentError
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (result.isSuccess) "API TEST PASSED" else "API TEST FAILED")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TestItemRow("Internet", result.internetPass)
                TestItemRow("Authentication", result.authPass)
                TestItemRow("Model Availability", result.modelPass)
                TestItemRow("TTS Synthesis", result.ttsPass)
                TestItemRow("Audio Response", result.audioPass)

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Response Time:", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "${String.format(java.util.Locale.US, "%.2f", result.responseTimeMs / 1000.0)}s",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!result.isSuccess) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                "Error: ${result.errorMessage}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                            result.suggestedSolution?.let {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Solution: $it",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("CLOSE") }
        }
    )
}

@Composable
fun TestItemRow(label: String, passed: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        if (passed) {
            Text("✓ PASS", color = AccentSuccess, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
        } else {
            Text("✗ FAIL", color = AccentError, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
        }
    }
}
