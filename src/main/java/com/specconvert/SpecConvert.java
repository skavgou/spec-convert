package com.specconvert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SpecConvert — CNCF Serverless Workflow spec 0.8 → 1.0 converter.
 *
 * Usage:
 *   java com.specconvert.SpecConvert <input-file> [output-file]
 *
 * If no output file is given the converted document is printed to stdout.
 * Both JSON (.json) and YAML (.yaml / .yml) input files are supported.
 * The output format matches the input format unless overridden.
 */
public class SpecConvert {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(
            YAMLFactory.builder()
                    .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                    .build())
            .enable(SerializationFeature.INDENT_OUTPUT);

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

        JsonNode root = read(inputPath);
        JsonNode converted = convert(root);

        boolean useYaml = isYaml(outputPath != null ? outputPath : inputPath);
        String output = serialise(converted, useYaml);

        if (outputPath != null) {
            Files.writeString(outputPath, output);
            System.out.println("Wrote converted file to: " + outputPath);
            return;
        }

        System.out.println(output);
    }

    /**
     * Parse a JSON or YAML file into a Jackson {@link JsonNode} tree.
     */
    public static JsonNode read(Path path) throws IOException {
        ObjectMapper mapper = isYaml(path) ? YAML_MAPPER : JSON_MAPPER;
        return mapper.readTree(path.toFile());
    }


    /**
     * Convert a 0.8 workflow document into a new 1.0 document structure.
     * The output is a freshly constructed object — nothing from the source
     * document is mutated or carried over verbatim.
     */
    public static JsonNode convert(JsonNode root) {
        if (!root.isObject()) {
            throw new IllegalArgumentException("Root of the workflow document must be a JSON object.");
        }

        ObjectNode out = JSON_MAPPER.createObjectNode();

        // 10 is split into "document" and "do" fields
        out.set("document", buildDocument((ObjectNode) root));
        out.set("do", buildDo((ObjectNode) root));
        return out;
    }

    // ---------------------------------------------------------------
    // 1.0 document builder
    // ---------------------------------------------------------------

    /**
     * Build the top-level document block from 0.8 fields
     */
    private static ObjectNode buildDocument(ObjectNode src) {
        ObjectNode document = JSON_MAPPER.createObjectNode();

        // dsl — always 1.0.0
        document.put("dsl", "1.0.0");
        System.err.println("[INFO] dsl set to 1.0.0");

        // namespace — use source value or fall back to "default"
        String namespace = src.has("namespace")
                ? src.get("namespace").asText()
                : "default";
        document.put("namespace", namespace);
        System.err.println("[INFO] namespace set to " + namespace);

        // name — mapped from "id"
        if (src.has("id")) {
            String docId = src.get("id").asText();
            document.put("name", docId);
            System.err.println("[INFO] name field set to " + docId);
        }

        // version — carried over as-is
        if (src.has("version")) {
            document.put("version", src.get("version").asText());
            System.err.println("[INFO] version field transferred");
        }

        return document;
    }

    /**
     * Build the do array from the 0.8 states array.
     * Each state becomes a named entry keyed by the state's name.
     * Currently handles:
     *   inject → set: uses state's 'data' field
     *   sleep → wait: uses state's time metrics
     *   switch → switch: copies state data as best as possible. 
     *      Cannot perform a true conversion, leaves warning for user
     */
    private static ArrayNode buildDo(ObjectNode src) {
        ArrayNode doArray = JSON_MAPPER.createArrayNode();

        JsonNode states = src.get("states");
        if (states == null || !states.isArray()) {
            return doArray;
        }

        for (JsonNode stateNode : states) {
            if (!stateNode.isObject()) continue;
            ObjectNode state = (ObjectNode) stateNode;

            String name = state.has("name") ? state.get("name").asText() : "unnamed";
            String type = state.has("type") ? state.get("type").asText() : "";

            ObjectNode stateEntry = JSON_MAPPER.createObjectNode();

            switch (type) {
                case "inject":
                    handleInject(state, stateEntry, name);
                    break;
                case "sleep":
                    String duration = state.has("duration") ? state.get("duration").asText() : "PT0S";
                    stateEntry.set(name, buildWait(duration));
                    break;
                case "switch":
                    handleSwitch(state, stateEntry, name);
                    break;
                default:
                    // Unknown state type — carry the raw state over under its name with a warning
                    System.err.println("[WARN] Unknown state type '" + type + "' for state '" + name + "'; carrying over as-is.");
                    stateEntry.set(name, state);
            }

            doArray.add(stateEntry);
        }

        return doArray;
    }

    private static void handleInject(ObjectNode state, ObjectNode stateEntry, String name) {
        ObjectNode setBlock = JSON_MAPPER.createObjectNode();
        if (state.has("data")) {
            setBlock.setAll((ObjectNode) state.get("data"));
        }
        ObjectNode setWrapper = JSON_MAPPER.createObjectNode();
        setWrapper.set("set", setBlock);
        stateEntry.set(name, setWrapper);
    }

    private static void handleSwitch(ObjectNode state, ObjectNode stateEntry, String name) {
        ArrayNode switchArray = JSON_MAPPER.createArrayNode();

        // --- named conditions from dataConditions ---
        JsonNode conditions = state.get("dataConditions");
        if (conditions != null && conditions.isArray()) {
            for (JsonNode condNode : conditions) {
                String caseName      = condNode.has("name")       ? toIdentifier(condNode.get("name").asText())      : "case";
                String rawExpression = condNode.has("condition")  ? condNode.get("condition").asText()               : "TODO";
                String transition    = condNode.has("transition") ? condNode.get("transition").asText()              : "TODO";

                ObjectNode caseBody = JSON_MAPPER.createObjectNode();
                String strippedExpression = stripExpressionWrapper(rawExpression);
                caseBody.put("when", strippedExpression);
                if (isWrappedExpression(rawExpression)) {
                    caseBody.put("_warning", "EL expression '" + strippedExpression + "' requires manual translation to jq syntax.");
                }
                caseBody.put("then", transition);

                ObjectNode caseEntry = JSON_MAPPER.createObjectNode();
                caseEntry.set(caseName, caseBody);
                switchArray.add(caseEntry);
            }
        }

        // --- default condition ---
        JsonNode defaultCond = state.get("defaultCondition");
        if (defaultCond != null && defaultCond.has("transition")) {
            ObjectNode defaultBody = JSON_MAPPER.createObjectNode();
            defaultBody.put("then", defaultCond.get("transition").asText());

            ObjectNode defaultEntry = JSON_MAPPER.createObjectNode();
            defaultEntry.set("default", defaultBody);
            switchArray.add(defaultEntry);
        }

        ObjectNode switchWrapper = JSON_MAPPER.createObjectNode();
        switchWrapper.set("switch", switchArray);
        stateEntry.set(name, switchWrapper);
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
     * Returns true when the expression uses the 0.8 EL wrapper (${ ... }).
     */
    private static boolean isWrappedExpression(String expression) {
        String trimmed = expression.trim();
        return trimmed.startsWith("${") && trimmed.endsWith("}");
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

    /**
     * Parse an ISO 8601 duration string (e.g. "P2DT3H4M") into total seconds,
     * then build a 1.0 wait block:
     * { "wait": { "seconds": 183840 } }
     */
    private static ObjectNode buildWait(String iso8601Duration) {
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
        long hours   = m.group(4) != null ? Long.parseLong(m.group(4)) : 0;
        long minutes = m.group(5) != null ? Long.parseLong(m.group(5)) : 0;
        long seconds = m.group(6) != null ? Long.parseLong(m.group(6)) : 0;

        // Convert each unit type to seconds
        years *= 365 * 86400;
        months *= 30 * 86400;
        days *= 86400;
        hours *= 3600;
        minutes *= 60;
        long totalSeconds = years + months + days + hours + minutes + seconds;

        ObjectNode waitBlock    = JSON_MAPPER.createObjectNode();
        ObjectNode waitContents = JSON_MAPPER.createObjectNode();
        waitContents.put("seconds", totalSeconds);
        waitBlock.set("wait", waitContents);
        return waitBlock;
    }


    private static void printUsage() {
        System.out.println("Usage: java -jar spec-convert.jar <input-file> [output-file]");
        System.out.println("Convert a CNCF Serverless Workflow spec 0.8 document to 1.0.");
    }

    /**
     * Serialise a JsonNode back to a String in either JSON or YAML.
     */
    public static String serialise(JsonNode node, boolean yaml) throws IOException {
        ObjectMapper mapper = yaml ? YAML_MAPPER : JSON_MAPPER;
        return mapper.writeValueAsString(node);
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
