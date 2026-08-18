package com.specconvert.transformer;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

// 0.8
import io.serverlessworkflow.api.actions.Action;
import io.serverlessworkflow.api.events.EventDefinition;
import io.serverlessworkflow.api.transitions.Transition;

// 1.0
import io.serverlessworkflow.api.types.CallFunction;
import io.serverlessworkflow.api.types.CallTask;
import io.serverlessworkflow.api.types.FlowDirective;
import io.serverlessworkflow.api.types.FunctionArguments;
import io.serverlessworkflow.api.types.Set;
import io.serverlessworkflow.api.types.SetTask;
import io.serverlessworkflow.api.types.SetTaskConfiguration;
import io.serverlessworkflow.api.types.SwitchCase;
import io.serverlessworkflow.api.types.SwitchItem;
import io.serverlessworkflow.api.types.SwitchTask;
import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.TaskItem;
import java.util.Map;

public class util {
    /**
     * Convert a single 0.8 action to a 1.0 TaskItem.
     *
     * A functionRef action becomes a CallFunction task keyed by the function's refName.
     * Arguments (JsonNode object) are copied as additional properties on FunctionArguments.
     */
    protected static TaskItem convertAction(Action action) {
        if (action.getFunctionRef() != null) {
            String refName = action.getFunctionRef().getRefName() != null
                    ? action.getFunctionRef().getRefName() : "function";

            FunctionArguments args = new FunctionArguments();
            JsonNode arguments = action.getFunctionRef().getArguments();
            if (arguments != null && arguments.isObject()) {
                arguments.fields().forEachRemaining(e -> args.setAdditionalProperty(e.getKey(), e.getValue()));
            }

            CallFunction callFn = new CallFunction()
                    .withCall(refName)
                    .withWith(args);

            String taskName = action.getName() != null ? action.getName() : refName;
            return new TaskItem(taskName, new Task().withCallTask(new CallTask().withCallFunction(callFn)));
        }

        // Fallback: unsupported action type — emit a set task with a warning marker
        System.err.println("[WARN] Action has no functionRef; emitting placeholder set task.");
        SetTaskConfiguration cfg = new SetTaskConfiguration();
        cfg.setAdditionalProperty("_warning", "unsupported action type");
        SetTask setTask = new SetTask().withSet(new Set().withSetTaskConfiguration(cfg));
        String taskName = action.getName() != null ? action.getName() : "unsupportedAction";
        return new TaskItem(taskName, new Task().withSetTask(setTask));
    }

    /**
     * Extract the next-state name from a 0.8 transition
     */
    protected static String transitionName(Transition t) {
        if (t == null || t.getNextState() == null) return "TODO";
        return t.getNextState();
    }

    /**
     * Convert a human-readable condition name like "Applicant is adult"
     * into a valid camelCase identifier like "applicantIsAdult" for use as a YAML key.
     */
    protected static String toIdentifier(String name) {
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
     * Return the last two dot-separated segments of a URI-style string as a space-separated pair,
     * suitable for passing to toIdentifier() to produce a unique camelCase name.
     * e.g. "com.hospital.vitals.temperature.high" → "temperature high"
     * e.g. "highTemperature" (no dots)            → "highTemperature"
     */
    protected static String lastTwoSegments(String type) {
        int last = type.lastIndexOf('.');
        if (last < 0) return type;
        int secondLast = type.lastIndexOf('.', last - 1);
        return (secondLast < 0 ? type.substring(0, last) : type.substring(secondLast + 1, last))
                + " " + type.substring(last + 1);
    }

    /**
     * Strip the 0.8 EL wrapper (${ ... }) from a condition string.
     * If the expression is not wrapped, it is returned as-is.
     * The inner content cannot be truly converted and will likely still need manual jq translation.
     */
    protected static String stripExpressionWrapper(String expression) {
        String trimmed = expression.trim();
        if (trimmed.startsWith("${") && trimmed.endsWith("}")) {
            String inner = trimmed.substring(2, trimmed.length() - 1).trim();
            System.err.println("[WARN] EL expression '" + inner + "' may need manual translation to jq syntax.");
            return inner;
        }
        return trimmed;
    }

    protected static void printUsage() {
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

    /**
     * Build the task list for the listen task's foreach iterator.
     *
     * When all event types share identical action lists the tasks are emitted directly.
     * When different event types have different action lists a switch task is emitted
     * that dispatches on "${ .item.type }" so only the matching branch executes.
     *
     * The iterator variable is named "item" (set in handleListen).
     */
    protected static List<TaskItem> buildForeachDo(
            String stateName,
            java.util.LinkedHashMap<String, List<Action>> actionsByType) {

        // Check whether every event type maps to the same action list
        List<List<Action>> allActionLists = new ArrayList<>(actionsByType.values());
        boolean uniform = allActionLists.stream()
                .allMatch(a -> actionListEquals(a, allActionLists.get(0)));

        if (uniform) {
            // All event types share the same actions — emit them directly
            List<TaskItem> items = new ArrayList<>();
            for (Action action : allActionLists.get(0)) {
                items.add(convertAction(action));
            }
            return items;
        }

        // Mixed actions — emit:
        //   1. a switch on ${ .item.type } whose each case jumps to a named do-task
        //   2. one named do-task per event type containing that type's actions
        // The switch + all named do-tasks sit together in foreach.do.
        List<TaskItem> foreachItems = new ArrayList<>();
        List<SwitchItem> switchItems = new ArrayList<>();

        for (Map.Entry<String, List<Action>> entry : actionsByType.entrySet()) {
            String cloudEventType = entry.getKey();
            List<Action> actions = entry.getValue();

            if (actions.isEmpty()) continue;

            // Derive a stable task name from the last two URI segments to avoid collisions
            // e.g. "com.hospital.vitals.temperature.high" → "temperatureHigh"
            String doTaskName = util.toIdentifier(util.lastTwoSegments(cloudEventType));

            // switch case: when this event type matches, jump to the named do-task
            SwitchCase switchCase = new SwitchCase()
                    .withWhen("${ .item.type == \"" + cloudEventType + "\" }")
                    .withThen(new FlowDirective().withString(doTaskName));
            switchItems.add(new SwitchItem(doTaskName, switchCase));

            // named do-task containing the converted actions for this event type
            List<TaskItem> actionTaskItems = new ArrayList<>();
            for (Action action : actions) {
                actionTaskItems.add(convertAction(action));
            }
            io.serverlessworkflow.api.types.DoTask doTask =
                    new io.serverlessworkflow.api.types.DoTask().withDo(actionTaskItems);
            foreachItems.add(new TaskItem(doTaskName, new Task().withDoTask(doTask)));
        }

        SwitchTask switchTask = new SwitchTask().withSwitch(switchItems);
        // Prepend the switch; named do-tasks follow so the switch can reference them by name
        foreachItems.add(0, new TaskItem("routeByEventType", new Task().withSwitchTask(switchTask)));
        return foreachItems;
    }

    /**
     * Shallow structural equality check for two action lists:
     * considers them equal when they have the same size and the same functionRef refNames
     * in the same order. Used to decide uniform vs mixed foreach dispatch.
     */
    protected static boolean actionListEquals(List<Action> a, List<Action> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            String ra = a.get(i).getFunctionRef() != null ? a.get(i).getFunctionRef().getRefName() : null;
            String rb = b.get(i).getFunctionRef() != null ? b.get(i).getFunctionRef().getRefName() : null;
            if (!java.util.Objects.equals(ra, rb)) return false;
        }
        return true;
    }

    /**
     * Build a map of event-definition name → CloudEvent type string
     * from the workflow's top-level events block.
     * Used by handleListen() to resolve eventRef names to CloudEvent types.
     */
    protected static Map<String, String> buildEventTypeMap(io.serverlessworkflow.api.Workflow src) {
        Map<String, String> map = new HashMap<>();
        if (src.getEvents() == null || src.getEvents().getEventDefs() == null) {
            return map;
        }
        for (EventDefinition def : src.getEvents().getEventDefs()) {
            if (def.getName() != null) {
                // Use the declared CloudEvent type if present, otherwise fall back to the name
                String type = def.getType() != null ? def.getType() : def.getName();
                map.put(def.getName(), type);
            }
        }
        return map;
    }

}
