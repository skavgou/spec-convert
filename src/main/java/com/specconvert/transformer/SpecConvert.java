package com.specconvert.transformer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// 0.8
import io.serverlessworkflow.api.mapper.JsonObjectMapper;
import io.serverlessworkflow.api.mapper.YamlObjectMapper;
import io.serverlessworkflow.api.states.CallbackState;
import io.serverlessworkflow.api.states.EventState;
import io.serverlessworkflow.api.states.ForEachState;
import io.serverlessworkflow.api.states.InjectState;
import io.serverlessworkflow.api.states.ParallelState;
import io.serverlessworkflow.api.states.OperationState;
import io.serverlessworkflow.api.states.SleepState;
import io.serverlessworkflow.api.states.SwitchState;
import io.serverlessworkflow.api.interfaces.State;

// 1.0
import io.serverlessworkflow.api.types.Document;
import io.serverlessworkflow.api.types.DurationInline;
import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.api.WorkflowFormat;
import io.serverlessworkflow.api.WorkflowWriter;
import java.util.Map;

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
            util.printUsage();
            return;
        }

        // Parse arguments: swf-migrate <input> [-o <output>]
        Path inputPath = null;
        Path outputPath = null;

        for (int i = 0; i < args.length; i++) {
            if ("-o".equals(args[i])) {
                if (i + 1 >= args.length) {
                    throw new IllegalArgumentException("-o requires a file path argument.");
                }
                outputPath = Path.of(args[++i]);
            } else if (inputPath == null) {
                inputPath = Path.of(args[i]);
            } else {
                throw new IllegalArgumentException("Unexpected argument: " + args[i]);
            }
        }

        if (inputPath == null) {
            throw new IllegalArgumentException("No input file specified.");
        }

        // Default output: <stem>-migrated.yaml alongside the input file
        if (outputPath == null) {
            String inputName = inputPath.getFileName().toString();
            String stem = inputName.contains(".")
                    ? inputName.substring(0, inputName.lastIndexOf('.'))
                    : inputName;
            Path parent = inputPath.getParent();
            outputPath = (parent != null ? parent : Path.of(".")).resolve(stem + "-migrated.yaml");
        }

        io.serverlessworkflow.api.Workflow wf08 = read(inputPath);
        io.serverlessworkflow.api.types.Workflow wf10 = convert(wf08);

        WorkflowFormat format = WorkflowFormat.fromPath(outputPath);

        // Suppress zero-valued duration fields (days:0, hours:0, etc.) from the output
        format.mapper().addMixIn(DurationInline.class, DurationInlineMixIn.class);

        WorkflowWriter.writeWorkflow(outputPath, wf10, format);
        System.out.println("Wrote converted file to: " + outputPath);
    }

    /**
     * Parse a JSON or YAML file into a 0.8 workflow instance.
     */
    public static io.serverlessworkflow.api.Workflow read(Path path) throws IOException {
        ObjectMapper mapper = util.isYaml(path) ? new YamlObjectMapper() : new JsonObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper.readValue(path.toFile(), io.serverlessworkflow.api.Workflow.class);
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
     *   inject    - set             (state data → set variables)
     *   sleep     - wait            (ISO 8601 duration → DurationInline)
     *   switch    - switch          (dataConditions + defaultCondition)
     *   parallel  - fork            (branches + completionType)
     *   event     - listen          (onEvents + exclusive flag)
     *   operation - call            (actions → call tasks; sequential = do, parallel = fork)
     *   forEach   - for             (inputCollection + iterationParam + actions)
     *   callback  - do[call+listen+switch]  (action → listen → conditional route)
     */
    private static List<TaskItem> buildDo(io.serverlessworkflow.api.Workflow src) {
        List<TaskItem> items = new ArrayList<>();

        if (src.getStates() == null) {
            return items;
        }

        // Build a name→type lookup from the workflow's top-level event definitions
        Map<String, String> eventTypeByName = util.buildEventTypeMap(src);

        for (State state : src.getStates()) {
            String stateName = state.getName() != null ? state.getName() : "unnamed";

            if (state instanceof InjectState) {
                items.add(Inject.handleInject(stateName, (InjectState) state));

            } else if (state instanceof SleepState) {
                items.add(new TaskItem(stateName, new Task().withWaitTask(Sleep.handleWait((SleepState) state))));

            } else if (state instanceof SwitchState) {
                items.add(Switch.handleSwitch(stateName, (SwitchState) state));

            } else if (state instanceof ParallelState) {
                items.add(Fork.handleFork(stateName, (ParallelState) state));

            } else if (state instanceof OperationState) {
                items.add(Operation.handleOperation(stateName, (OperationState) state));

            } else if (state instanceof EventState) {
                items.add(Listen.handleListen(stateName, (EventState) state, eventTypeByName));

            } else if (state instanceof ForEachState) {
                items.add(ForEach.handleForEach(stateName, (ForEachState) state));

            } else if (state instanceof CallbackState) {
                items.add(Callback.handleCallback(stateName, (CallbackState) state, eventTypeByName));

            } else {
                System.err.println("[WARN] Unsupported state type for state '"
                        + stateName + "' (" + state.getClass().getSimpleName() + "); skipping.");
            }
        }

        return items;
    }
  
}
