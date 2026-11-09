package com.example.data.model

enum class DiagnosticStatus {
    PASS,
    WARNING,
    FAIL,
    NOT_TESTED
}

data class DiagnosticComponentResult(
    val component: String,
    val status: DiagnosticStatus = DiagnosticStatus.NOT_TESTED,
    val title: String = component,
    val errorCode: String? = null,
    val message: String = "Not tested yet",
    val technicalDetails: String? = null,
    val possibleCause: String? = null,
    val suggestedSolution: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class FullDiagnosticSummary(
    val timestamp: Long = System.currentTimeMillis(),
    val isRunning: Boolean = false,
    val results: List<DiagnosticComponentResult> = emptyList()
) {
    val passedCount: Int get() = results.count { it.status == DiagnosticStatus.PASS }
    val warningCount: Int get() = results.count { it.status == DiagnosticStatus.WARNING }
    val failedCount: Int get() = results.count { it.status == DiagnosticStatus.FAIL }

    val overallStatus: DiagnosticStatus
        get() = when {
            results.isEmpty() -> DiagnosticStatus.NOT_TESTED
            failedCount > 0 -> DiagnosticStatus.FAIL
            warningCount > 0 -> DiagnosticStatus.WARNING
            passedCount > 0 -> DiagnosticStatus.PASS
            else -> DiagnosticStatus.NOT_TESTED
        }
}
