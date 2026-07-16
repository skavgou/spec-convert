package com.specconvert;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 0.8
import io.serverlessworkflow.api.mapper.JsonObjectMapper;
import io.serverlessworkflow.api.mapper.YamlObjectMapper;
import io.serverlessworkflow.api.states.InjectState;
import io.serverlessworkflow.api.states.SleepState;
import io.serverlessworkflow.api.states.SwitchState;
import io.serverlessworkflow.api.switchconditions.DataCondition;
import io.serverlessworkflow.api.defaultdef.DefaultConditionDefinition;
import io.serverlessworkflow.api.interfaces.State;
import io.serverlessworkflow.api.transitions.Transition;

// 1.0
import io.serverlessworkflow.api.types.Document;
import io.serverlessworkflow.api.types.DurationInline;
import io.serverlessworkflow.api.types.FlowDirective;
import io.serverlessworkflow.api.types.Set;
import io.serverlessworkflow.api.types.SetTask;
import io.serverlessworkflow.api.types.SetTaskConfiguration;
import io.serverlessworkflow.api.types.SwitchCase;
import io.serverlessworkflow.api.types.SwitchItem;
import io.serverlessworkflow.api.types.SwitchTask;
import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.api.types.TimeoutAfter;
import io.serverlessworkflow.api.types.WaitTask;
import io.serverlessworkflow.api.WorkflowFormat;
import io.serverlessworkflow.api.WorkflowWriter;

// Workflow10 = io.serverlessworkflow.api.types.Workflow  (1.0 output)
// Workflow08 = io.serverlessworkflow.api.Workflow        (0.8 input, referenced by FQN)

/** MixIn that suppresses zero-valued fields on DurationInline during serialisation. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
interface DurationInlineMixIn {}

/**
 * SpecConvert — CNCF Serverless Workflow spec 0.8 -> 1.0 converter.
 *
 * Input  is parsed via the 0.8 SDK (serverlessworkflow-api 4.1.0.Final).
 * Output is built via the 1.0 SDK (serverlessworkflow-types 7.25.0.Final).
 *
 * Usage:
 *   java -jar spec-convert.jar <input-file> [output-file]
 *
 * If no output file is given the converted document is printed to stdout.
 * Both JSON (.json) and YAML (.yaml / .yml) input files are supported.
 * The output format matches the input format unless overridden.
 */
public class SpecConvert {

    public static void main(String[] args) throws IOException {
        if (args.length == 0 || "-h".equals(args[0]) || "--help".equals(args[0])) {
            printUsage();
            return;
        }

        if (args.length > 2) {
            throw new IllegalArgumentException("Expected 1 or 2 arguments.");
        }

        Path inputPath = Path.of(args[0]);
        Path outputPath = args.length == 2 ? Path.of(args[1]) : null;

        io.serverlessworkflow.api.Workflow wf08 = read(inputPath);
        io.serverlessworkflow.api.types.Workflow wf10 = convert(wf08);

        WorkflowFormat format = outputPath != null
                ? WorkflowFormat.fromPath(outputPath)
                : WorkflowFormat.fromPath(inputPath);

        // Suppress zero-valued duration fields (days:0, hours:0, etc.) from the output
        format.mapper().addMixIn(DurationInline.class, DurationInlineMixIn.class);

        if (outputPath != null) {
            WorkflowWriter.writeWorkflow(outputPath, wf10, format);
            System.out.println("Wrote converted file to: " + outputPath);
        } else {
            System.out.println(WorkflowWriter.workflowAsString(wf10, format));
        }
    }

    /**
     * Parse a JSON or YAML file into a 0.8 workflow instance.
     */
    public static io.serverlessworkflow.api.Workflow read(Path path) throws IOException {
        if (isYaml(path)) {
            return new YamlObjectMapper().readValue(path.toFile(), io.serverlessworkflow.api.Workflow.class);
        } else {
            return new JsonObjectMapper().readValue(path.toFile(), io.serverlessworkflow.api.Workflow.class);
        }
    }

    /**
     * Convert a parsed 0.8 workflow into 1.0.
     */
    public static io.serverlessworkflow.api.types.Workflow convert(
            io.serverlessworkflow.api.Workflow src) {

        Document document = buildDocument(src);
        List<TaskItem> doList = buildDo(src);
        return new io.serverlessworkflow.api.types.Workflow(document, doList);
    }

    // ---------------------------------------------------------------
    // 1.0 document builder
    // ---------------------------------------------------------------

    /**
     * Build the top-level document block from 0.8 fields
     */
    private static Document buildDocument(io.serverlessworkflow.api.Workflow src) {
        // dsl — always "1.0.0" for output
        String dsl = "1.0.0";
        System.err.println("[INFO] dsl set to " + dsl);

        // namespace — 0.8 spec has no namespace field; fall back to "default"
        String namespace = "default";
        System.err.println("[INFO] namespace set to " + namespace);

        // name — mapped from 0.8 "id"
        String name = src.getId() != null ? src.getId() : "unnamed";
        System.err.println("[INFO] name set to " + name);

        // version — carried over as-is
        String version = src.getVersion() != null ? src.getVersion() : "0.0.1";
        System.err.println("[INFO] version set to " + version);

        return new Document(dsl, namespace, name, version);
    }

    // -----------------------------------------------------------------------
    // 1.0 do-list builder
    // -----------------------------------------------------------------------

    /**
     * Build the 1.0 do block from the 0.8 states list.
     * Each state becomes a TaskItem keyed by the state's name.
     *
     * Handled mappings:
     *   inject  → set    (state data → set variables)
     *   sleep   → wait   (ISO 8601 duration → DurationInline)
     *   switch  → switch (dataConditions + defaultCondition)
     */
    private static List<TaskItem> buildDo(io.serverlessworkflow.api.Workflow src) {
        List<TaskItem> items = new ArrayList<>();

        if (src.getStates() == null) {
            return items;
        }

        for (State state : src.getStates()) {
            String stateName = state.getName() != null ? state.getName() : "unnamed";

            if (state instanceof InjectState) {
                items.add(handleInject(stateName, (InjectState) state));

            } else if (state instanceof SleepState) {
                String duration = ((SleepState) state).getDuration();
                if (duration == null) duration = "PT0S";
                items.add(new TaskItem(stateName, new Task().withWaitTask(handleWait(duration))));

            } else if (state instanceof SwitchState) {
                items.add(handleSwitch(stateName, (SwitchState) state));

            } else {
                System.err.println("[WARN] Unsupported state type for state '"
                        + stateName + "' (" + state.getClass().getSimpleName() + "); skipping.");
            }
        }

        return items;
    }

    private static TaskItem handleInject(String name, InjectState state) {
        SetTaskConfiguration cfg = new SetTaskConfiguration();

        if (state.getData() != null && state.getData().isObject()) {
            state.getData().fields().forEachRemaining(entry ->
                    cfg.setAdditionalProperty(entry.getKey(), entry.getValue())
            );
        }

        SetTask setTask = new SetTask().withSet(new Set().withSetTaskConfiguration(cfg));
        return new TaskItem(name, new Task().withSetTask(setTask));
    }

    /**
     * Parse an ISO 8601 duration string (e.g. "P2DT3H4M") into total seconds,
     * then build a 1.0 wait block:
     * { "wait": { "seconds": 183840 } }
     */
    private static WaitTask handleWait(String iso8601Duration) {
        Pattern pattern = Pattern.compile(
            "P(?:(\\d+)Y)?(?:(\\d+)M)?(?:(\\d+)D)?(?:T(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?)?"
        );
        Matcher m = pattern.matcher(iso8601Duration);

        if (!m.matches()) {
            throw new IllegalArgumentException("Invalid ISO 8601 duration: " + iso8601Duration);
        }

        long years   = m.group(1) != null ? Long.parseLong(m.group(1)) : 0;
        long months  = m.group(2) != null ? Long.parseLong(m.group(2)) : 0;
        long days    = m.group(3) != null ? Long.parseLong(m.group(3)) : 0;
        long  hours   = m.group(4) != null ? Integer.parseInt(m.group(4)) : 0;
        long  minutes = m.group(5) != null ? Integer.parseInt(m.group(5)) : 0;
        long  seconds = m.group(6) != null ? Integer.parseInt(m.group(6)) : 0;

        // Fold years/months into days (approximate)
        long totalDays = days + years * 365L + months * 30L;

        // Convert each unit type to seconds
        years *= 365 * 86400;
        months *= 30 * 86400;
        days *= 86400;
        hours *= 3600;
        minutes *= 60;
        int totalSeconds = (int) (totalDays + hours + minutes + seconds);

        DurationInline dur = new DurationInline()
                .withSeconds(totalSeconds);

        return new WaitTask().withWait(new TimeoutAfter().withDurationInline(dur));
    }

    private static TaskItem handleSwitch(String name, SwitchState state) {
        List<SwitchItem> switchItems = new ArrayList<>();

        // Named data conditions
        if (state.getDataConditions() != null) {
            for (DataCondition cond : state.getDataConditions()) {
                String caseName      = cond.getName() != null ? toIdentifier(cond.getName()) : "case";
                String rawExpression = cond.getCondition() != null ? cond.getCondition() : "TODO";
                String nextState     = transitionName(cond.getTransition());

                String strippedExpression = stripExpressionWrapper(rawExpression);
                SwitchCase switchCase = new SwitchCase()
                        .withWhen(strippedExpression)
                        .withThen(new FlowDirective().withString(nextState));

                switchItems.add(new SwitchItem(caseName, switchCase));
            }
        }

        // Default condition (no "when" predicate)
        DefaultConditionDefinition def = state.getDefaultCondition();
        if (def != null) {
            String nextState = transitionName(def.getTransition());
            SwitchCase defaultCase = new SwitchCase()
                    .withThen(new FlowDirective().withString(nextState));
            switchItems.add(new SwitchItem("default", defaultCase));
        }

        SwitchTask switchTask = new SwitchTask().withSwitch(switchItems);
        return new TaskItem(name, new Task().withSwitchTask(switchTask));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Extract the next-state name from a 0.8 transition
     */
    private static String transitionName(Transition t) {
        if (t == null || t.getNextState() == null) return "TODO";
        return t.getNextState();
    }

    /**
     * Convert a human-readable condition name like "Applicant is adult"
     * into a valid camelCase identifier like "applicantIsAdult" for use as a YAML key.
     */
    private static String toIdentifier(String name) {
        String[] words = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String word = words[i].replaceAll("[^a-zA-Z0-9]", "");
            if (word.isEmpty()) continue;
            if (i == 0) {
                sb.append(word.toLowerCase());
            } else {
                sb.append(Character.toUpperCase(word.charAt(0)));
                sb.append(word.substring(1).toLowerCase());
            }
        }
        return sb.isEmpty() ? "case" : sb.toString();
    }

    /**
     * Strip the 0.8 EL wrapper (${ ... }) from a condition string.
     * If the expression is not wrapped, it is returned as-is.
     * The inner content cannot be truly converted and will likely still need manual jq translation.
     */
    private static String stripExpressionWrapper(String expression) {
        String trimmed = expression.trim();
        if (trimmed.startsWith("${") && trimmed.endsWith("}")) {
            String inner = trimmed.substring(2, trimmed.length() - 1).trim();
            System.err.println("[WARN] EL expression '" + inner + "' may need manual translation to jq syntax.");
            return inner;
        }
        return trimmed;
    }

    private static void printUsage() {
        System.out.println("Usage: java -jar spec-convert.jar <input-file> [output-file]");
        System.out.println("Convert a CNCF Serverless Workflow spec 0.8 document to 1.0.");
    }

    /**
     * Returns true when the path has a .yaml or yml etension extension.
     */
    static boolean isYaml(Path path) {
        if (path == null) return false;
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".yaml") || name.endsWith(".yml");
    }
}
