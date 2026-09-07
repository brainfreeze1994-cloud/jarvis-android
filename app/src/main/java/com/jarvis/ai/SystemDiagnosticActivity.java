package com.jarvis.ai;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * H.E.N.R.Y. — System Diagnostic, Audit & Verification Activity
 *
 * Provides a holographic J.A.R.V.I.S.-style telemetry HUD for live self-testing,
 * auto-repair, and formal PDF audit report export.
 */
public class SystemDiagnosticActivity extends AppCompatActivity {

    private TextView tvHealthScoreNum;
    private TextView tvIntegrityBadge;
    private TextView tvAuditTimestamp;
    private TextView tvAuditDuration;
    private TextView tvAuditCounts;
    private ProgressBar progressAudit;
    private TextView btnRunFullAudit;
    private TextView btnAutoRepair;
    private TextView btnExportPdf;
    private TextView tvTerminalLogs;
    private LinearLayout containerSubsystems;

    private SystemDiagnosticEngine.DiagnosticReport latestReport;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final StringBuilder logAccumulator = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_diagnostic);

        initViews();
        initListeners();

        // Run full audit immediately on launch
        executeSystemAudit();
    }

    private void initViews() {
        tvHealthScoreNum    = findViewById(R.id.tv_health_score_num);
        tvIntegrityBadge    = findViewById(R.id.tv_integrity_badge);
        tvAuditTimestamp    = findViewById(R.id.tv_audit_timestamp);
        tvAuditDuration     = findViewById(R.id.tv_audit_duration);
        tvAuditCounts       = findViewById(R.id.tv_audit_counts);
        progressAudit       = findViewById(R.id.progress_audit);
        btnRunFullAudit     = findViewById(R.id.btn_run_full_audit);
        btnAutoRepair       = findViewById(R.id.btn_auto_repair);
        btnExportPdf        = findViewById(R.id.btn_diagnostic_export_pdf);
        tvTerminalLogs      = findViewById(R.id.tv_terminal_logs);
        containerSubsystems = findViewById(R.id.container_subsystems);
    }

    private void initListeners() {
        View btnBack = findViewById(R.id.btn_diagnostic_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnRunFullAudit != null) {
            btnRunFullAudit.setOnClickListener(v -> executeSystemAudit());
        }

        if (btnAutoRepair != null) {
            btnAutoRepair.setOnClickListener(v -> executeAutoRepair());
        }

        if (btnExportPdf != null) {
            btnExportPdf.setOnClickListener(v -> exportVerificationReport());
        }
    }

    private void executeSystemAudit() {
        if (btnRunFullAudit != null) {
            btnRunFullAudit.setEnabled(false);
            btnRunFullAudit.setText("SCANNING…");
        }
        if (progressAudit != null) {
            progressAudit.setVisibility(View.VISIBLE);
            progressAudit.setProgress(0);
        }

        containerSubsystems.removeAllViews();
        logAccumulator.setLength(0);
        tvTerminalLogs.setText("[INIT] Initiating H.E.N.R.Y. Core Audit...\n");

        SystemDiagnosticEngine.runFullAudit(this, new SystemDiagnosticEngine.AuditCallback() {
            @Override
            public void onProgress(int step, int totalSteps, String moduleName, String logMsg) {
                if (progressAudit != null) progressAudit.setProgress(step);
                logAccumulator.append(logMsg).append("\n");
                // Keep the most recent 12 lines visible in the terminal HUD
                String[] lines = logAccumulator.toString().split("\n");
                int start = Math.max(0, lines.length - 12);
                StringBuilder visible = new StringBuilder();
                for (int i = start; i < lines.length; i++) {
                    visible.append(lines[i]).append("\n");
                }
                tvTerminalLogs.setText(visible.toString());
            }

            @Override
            public void onSubsystemFinished(SystemDiagnosticEngine.SubsystemResult result) {
                addSubsystemCard(result);
            }

            @Override
            public void onAuditComplete(SystemDiagnosticEngine.DiagnosticReport report) {
                latestReport = report;
                if (progressAudit != null) progressAudit.setVisibility(View.GONE);
                if (btnRunFullAudit != null) {
                    btnRunFullAudit.setEnabled(true);
                    btnRunFullAudit.setText("⚡ RUN FULL AUDIT");
                }

                // Update Health Header
                tvHealthScoreNum.setText(report.overallHealthScore + "%");
                tvIntegrityBadge.setText(report.integrityLevel);
                int badgeColor = report.overallHealthScore >= 90 ? 0xFF00FFCC
                        : report.overallHealthScore >= 70 ? 0xFF00D4FF : 0xFFFF4757;
                tvIntegrityBadge.setTextColor(badgeColor);
                tvHealthScoreNum.setTextColor(badgeColor);

                String dateStr = new SimpleDateFormat("MMM d, yyyy • HH:mm:ss", Locale.US)
                        .format(new Date(report.timestamp));
                tvAuditTimestamp.setText("Audit Verified: " + dateStr);
                tvAuditDuration.setText(String.format(Locale.US, "Execution Latency: %d ms • %d Subsystems Audited",
                        report.totalAuditDurationMs, report.subsystems.size()));
                if (tvAuditCounts != null) {
                    tvAuditCounts.setText(String.format(Locale.US,
                            "Tests: %d • Passed: %d • Warnings: %d • Failed: %d • Not Config: %d",
                            report.totalTests, report.passedCount, report.warningCount, report.failedCount, report.notConfiguredCount));
                }
            }

            @Override
            public void onError(String error) {
                if (progressAudit != null) progressAudit.setVisibility(View.GONE);
                if (btnRunFullAudit != null) {
                    btnRunFullAudit.setEnabled(true);
                    btnRunFullAudit.setText("⚡ RUN FULL AUDIT");
                }
                Toast.makeText(SystemDiagnosticActivity.this, "Audit notice: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addSubsystemCard(SystemDiagnosticEngine.SubsystemResult result) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View card = inflater.inflate(R.layout.item_subsystem_diagnostic, containerSubsystems, false);

        TextView tvName = card.findViewById(R.id.tv_subsystem_name);
        TextView tvBadge = card.findViewById(R.id.tv_subsystem_badge);
        TextView tvTest = card.findViewById(R.id.tv_subsystem_test);
        TextView tvSummary = card.findViewById(R.id.tv_subsystem_summary);
        TextView tvError = card.findViewById(R.id.tv_subsystem_error);
        TextView tvFix = card.findViewById(R.id.tv_subsystem_fix);
        TextView tvLatency = card.findViewById(R.id.tv_subsystem_latency);
        TextView tvDeps = card.findViewById(R.id.tv_subsystem_deps);
        LinearLayout detailsLayout = card.findViewById(R.id.layout_telemetry_details);

        tvName.setText(result.name);
        tvBadge.setText(result.statusBadge);
        tvBadge.setTextColor(result.statusColor);
        if (tvTest != null) tvTest.setText("Test: " + result.testName);
        tvSummary.setText(result.summary);

        if (tvError != null) {
            if (result.errorMessage != null && !result.errorMessage.isEmpty()) {
                tvError.setVisibility(View.VISIBLE);
                tvError.setText("⚠ Reason: " + result.errorMessage);
            } else {
                tvError.setVisibility(View.GONE);
            }
        }

        if (tvFix != null) {
            if (result.recommendedFix != null && !result.recommendedFix.isEmpty()) {
                tvFix.setVisibility(View.VISIBLE);
                tvFix.setText("Suggested action: " + result.recommendedFix);
            } else {
                tvFix.setVisibility(View.GONE);
            }
        }

        if (tvLatency != null) {
            tvLatency.setText(String.format(Locale.US, "⏱️ %d ms • %s", result.latencyMs, result.category));
        }

        if (tvDeps != null && result.dependencyStatus != null) {
            tvDeps.setText("Deps: " + result.dependencyStatus);
        }

        if (result.telemetryDetails != null && !result.telemetryDetails.isEmpty()) {
            detailsLayout.removeAllViews();
            for (String detail : result.telemetryDetails) {
                TextView tvDetail = new TextView(this);
                tvDetail.setText("▸ " + detail);
                tvDetail.setTextColor(0xFF8AB4F8);
                tvDetail.setTextSize(10.5f);
                tvDetail.setPadding(0, 2, 0, 2);
                detailsLayout.addView(tvDetail);
            }
        } else {
            detailsLayout.setVisibility(View.GONE);
        }

        containerSubsystems.addView(card);
    }

    private void executeAutoRepair() {
        if (btnAutoRepair != null) {
            btnAutoRepair.setEnabled(false);
            btnAutoRepair.setText("REPAIRING…");
        }

        logAccumulator.append("\n[AUTO-REPAIR] Starting autonomous system stabilization sequence...\n");
        tvTerminalLogs.setText(logAccumulator.toString());

        SystemDiagnosticEngine.performAutoRepair(this, new SystemDiagnosticEngine.AutoRepairCallback() {
            @Override
            public void onRepairProgress(String step) {
                logAccumulator.append("[REPAIR] ").append(step).append("\n");
                tvTerminalLogs.setText(logAccumulator.toString());
            }

            @Override
            public void onRepairComplete(int resolvedIssuesCount, String summary) {
                if (btnAutoRepair != null) {
                    btnAutoRepair.setEnabled(true);
                    btnAutoRepair.setText("🛠️ AUTO-REPAIR");
                }
                logAccumulator.append("[REPAIR COMPLETE] ").append(resolvedIssuesCount).append(" system routines stabilized.\n");
                tvTerminalLogs.setText(logAccumulator.toString());

                Toast.makeText(SystemDiagnosticActivity.this,
                        "Auto-repair completed: " + resolvedIssuesCount + " systems stabilized.",
                        Toast.LENGTH_LONG).show();

                // Re-run the diagnostic to show freshly stabilized results
                mainHandler.postDelayed(() -> executeSystemAudit(), 500);
            }
        });
    }

    private void exportVerificationReport() {
        if (latestReport == null) {
            Toast.makeText(this, "Audit in progress. Please wait for completion.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File pdfFile = SystemDiagnosticEngine.exportAuditPdfReport(this, latestReport);
            Toast.makeText(this, "Verified Report Exported: " + pdfFile.getName(), Toast.LENGTH_LONG).show();

            // Offer to view / share the file
            Uri contentUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
                    pdfFile
            );

            Intent viewIntent = new Intent(Intent.ACTION_VIEW);
            viewIntent.setDataAndType(contentUri, "application/pdf");
            viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Intent chooser = Intent.createChooser(viewIntent, "Open H.E.N.R.Y. System Audit Report");
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(chooser);

        } catch (Exception e) {
            Toast.makeText(this, "Error generating PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
