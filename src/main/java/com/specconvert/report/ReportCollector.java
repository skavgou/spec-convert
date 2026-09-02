package com.specconvert.report;

import com.specconvert.report.MigrationReport.Category;
import com.specconvert.report.MigrationReport.Issue;
import com.specconvert.report.MigrationReport.ManualTask;
import com.specconvert.report.MigrationReport.Severity;

/**
 * Call-scoped collector for migration issues and manual tasks.
 *
 * Usage pattern:
 *   1. SpecConvert calls ReportCollector.init(sourceFile) before conversion.
 *   2. Each transformer calls ReportCollector.get().addIssue(...) / addManualTask(...).
 *   3. SpecConvert calls ReportCollector.get().finalise(totalStates, migratedStates)
 *      and then retrieves the completed report via ReportCollector.get().getReport().
 */
public class ReportCollector {

    private static ReportCollector instance;

    private final MigrationReport report = new MigrationReport();
    private int nextTaskId = 1;

    private ReportCollector(String sourceFile) {
        report.summary.sourceFile    = sourceFile;
        report.summary.migrationDate = java.time.Instant.now().toString();
    }

    /** Initialise a fresh collector for a new conversion run. */
    public static void init(String sourceFile) {
        instance = new ReportCollector(sourceFile);
    }

    /** Retrieve the active collector. Must be called after init(). */
    public static ReportCollector get() {
        if (instance == null) {
            throw new IllegalStateException("ReportCollector not initialised — call init() first.");
        }
        return instance;
    }

    // ------------------------------------------------------------------
    // Recording helpers
    // ------------------------------------------------------------------

    public void addIssue(Severity severity, Category category, String sourceLocation,
                         String message, String original, String converted, String actionRequired) {
        report.issues.add(new Issue(severity, category, sourceLocation,
                message, original, converted, actionRequired));
        if (severity == Severity.WARNING) report.summary.statistics.warningsCount++;
        if (severity == Severity.ERROR)   report.summary.statistics.errorsCount++;
    }

    /** Convenience overload for issues with no before/after values. */
    public void addIssue(Severity severity, Category category,
                         String sourceLocation, String message) {
        addIssue(severity, category, sourceLocation, message, null, null, null);
    }

    public void addManualTask(String priority, String description,
                              String details, String sourceReference) {
        report.manualTasks.add(new ManualTask(nextTaskId++, priority,
                description, details, sourceReference));
    }

    // ------------------------------------------------------------------
    // Finalisation
    // ------------------------------------------------------------------

    /** Compute derived summary fields and set the overall status. */
    public void finalise(int totalStates, int migratedStates) {
        report.summary.statistics.totalStates    = totalStates;
        report.summary.statistics.migratedStates = migratedStates;

        int errors   = report.summary.statistics.errorsCount;
        int warnings = report.summary.statistics.warningsCount;

        if (errors > 0) {
            report.summary.overallStatus = "partial";
        } else if (warnings > 0) {
            report.summary.overallStatus = "success_with_warnings";
        } else {
            report.summary.overallStatus = "success";
        }
    }

    public MigrationReport getReport() {
        return report;
    }
}
