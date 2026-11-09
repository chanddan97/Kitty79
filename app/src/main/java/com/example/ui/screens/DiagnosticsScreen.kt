package com.example.ui.screens

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
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DiagnosticComponentResult
import com.example.data.model.DiagnosticStatus
import com.example.ui.components.DiagnosticDetailDialog
import com.example.ui.theme.AccentError
import com.example.ui.theme.AccentSuccess
import com.example.ui.theme.AccentWarning
import com.example.viewmodel.TtsViewModel

@Composable
fun DiagnosticsScreen(
    viewModel: TtsViewModel,
    modifier: Modifier = Modifier
) {
    val diagnosticSummary by viewModel.diagnosticSummary.collectAsState()
    val progressMessage by viewModel.diagnosticProgress.collectAsState()
    val selectedDetail by viewModel.selectedDiagnosticDetail.collectAsState()

    val defaultComponents = listOf(
        "App Initialization",
        "Android Compatibility",
        "Internet",
        "API Key",
        "Authentication",
        "Selected Model",
        "TTS Endpoint",
        "Voice",
        "Audio Response",
        "Audio Decoder",
        "Storage",
        "Audio Player",
        "History Database"
    )

    // Merge existing results with default components so all 13 are visible even before testing!
    val displayedResults = defaultComponents.map { compName ->
        diagnosticSummary.results.firstOrNull { it.component == compName }
            ?: DiagnosticComponentResult(component = compName, status = DiagnosticStatus.NOT_TESTED)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "APP DIAGNOSTICS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Real-time automated component health inspection",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Health Status Hero Card
        val (badgeColor, badgeText) = when (diagnosticSummary.overallStatus) {
            DiagnosticStatus.PASS -> AccentSuccess to "🟢 SYSTEM HEALTHY"
            DiagnosticStatus.WARNING -> AccentWarning to "🟡 MINOR WARNINGS"
            DiagnosticStatus.FAIL -> AccentError to "🔴 ISSUES DETECTED"
            DiagnosticStatus.NOT_TESTED -> MaterialTheme.colorScheme.onSurfaceVariant to "⚪ NOT TESTED"
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("diagnostic_status_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Passed: ${diagnosticSummary.passedCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentSuccess,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Warn: ${diagnosticSummary.warningCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentWarning,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Errors: ${diagnosticSummary.failedCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentError,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (diagnosticSummary.isRunning) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = progressMessage.ifEmpty { "Executing diagnostic routines..." },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Run Full Diagnostic Button
        Button(
            onClick = { viewModel.runFullDiagnostics() },
            enabled = !diagnosticSummary.isRunning,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("run_diagnostics_button")
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("RUN FULL DIAGNOSTIC", fontWeight = FontWeight.Bold)
        }

        // Components List
        Text(
            text = "SYSTEM COMPONENTS (${displayedResults.size})",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(displayedResults) { res ->
                DiagnosticComponentRow(
                    result = res,
                    onClick = { viewModel.selectDiagnosticDetail(res) }
                )
            }
        }
    }

    // Detail Dialog
    selectedDetail?.let { detail ->
        DiagnosticDetailDialog(
            result = detail,
            onRetry = { viewModel.runFullDiagnostics() },
            onDismiss = { viewModel.selectDiagnosticDetail(null) }
        )
    }
}

@Composable
fun DiagnosticComponentRow(
    result: DiagnosticComponentResult,
    onClick: () -> Unit
) {
    val (statusColor, statusLabel) = when (result.status) {
        DiagnosticStatus.PASS -> AccentSuccess to "● PASS"
        DiagnosticStatus.WARNING -> AccentWarning to "● WARNING"
        DiagnosticStatus.FAIL -> AccentError to "● FAIL"
        DiagnosticStatus.NOT_TESTED -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) to "○ NOT TESTED"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .testTag("diagnostic_row_${result.component.replace(" ", "_").lowercase()}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.component,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = result.message,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
