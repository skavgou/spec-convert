# SpecConvert

CNCF Serverless Workflow **0.8 → 1.0** converter.

A plain-Java command-line tool — no build tool or application framework required. Just a JDK and the bundled Jackson JARs in `lib/`.

---

## Requirements

- Java 17+
- The `lib/` JARs are already included (Jackson 2.19.0 + SnakeYAML 2.3)

---

## Build

```bash
./build.sh
```

Compiles the source and produces `out/spec-convert.jar`.

---

## Run

```bash
./run.sh <input-file> [output-file]
```

- Both JSON (`.json`) and YAML (`.yaml` / `.yml`) inputs are supported.
- If no output file is given the result is printed to stdout.
- The output format matches the input format, inferred from the file extension.

**Examples**

```bash
# Print converted JSON to stdout
./run.sh samples/hello.json

# Write converted YAML to a file
./run.sh samples/hello.yaml results/hello-converted.yaml
```

---

## Manual compile / run (without the scripts)

```bash
CP="lib/jackson-core-2.19.0.jar:lib/jackson-annotations-2.19.0.jar:lib/jackson-databind-2.19.0.jar:lib/jackson-dataformat-yaml-2.19.0.jar:lib/snakeyaml-2.3.jar"

# Compile
javac -cp "$CP" -d out src/main/java/com/specconvert/SpecConvert.java

# Run
java -cp "out:$CP" com.specconvert.SpecConvert samples/hello.json
```

---

## Conversion details

See [`CONVERSION_NOTES.md`](CONVERSION_NOTES.md) for a full description of the field mappings and state-type handling.
