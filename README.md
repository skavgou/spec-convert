# SpecConvert

CNCF Serverless Workflow **0.8 → 1.0** converter.

A plain-Java command-line tool built with Maven.

---

## Requirements

- Java 17+
- Maven 3.8+

---

## First-time setup

The 0.8 SDK is distributed as a locally-shaded jar and is not available on Maven Central. Install it into your local Maven repository once before building:

```bash
mvn install:install-file \
  -Dfile=lib/serverlessworkflow-api-v08-4.1.0.Final.jar \
  -DgroupId=io.serverlessworkflow.v08 \
  -DartifactId=serverlessworkflow-api \
  -Dversion=4.1.0.Final \
  -Dpackaging=jar
```

All other dependencies are pulled automatically from Maven Central on the first build.

---

## Build

```bash
./build.sh
```

Or directly with Maven:

```bash
mvn package
```

Produces `target/spec-convert.jar` — a self-contained fat jar with all dependencies included.

---

## Run

```bash
./run.sh <input-file> [output-file]
```

Or directly with Java:

```bash
java -jar target/spec-convert.jar <input-file> [output-file]
```

- Both JSON (`.json`) and YAML (`.yaml` / `.yml`) inputs are supported.
- If no output file is given the result is printed to stdout.
- The output format matches the input format, inferred from the file extension.

**Examples**

```bash
# Print converted JSON to stdout
./run.sh samples/hello.json

# Write converted JSON to a file
./run.sh samples/hello.json results/hello-converted.json
```

---

## Clean

```bash
mvn clean
```

---

## Conversion details

See [`CONVERSION_NOTES.md`](CONVERSION_NOTES.md) for a full description of the field mappings and state-type handling.
