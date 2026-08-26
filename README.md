# swf-migrate

CNCF Serverless Workflow **0.8 → 1.0** converter.

---

## Install (no Java required)

Download a pre-built binary from the (CURRENTLY NOT WORKING)[releases page](../../releases) and place it on your PATH, or use the one-liner for your platform.

**macOS / Linux**
```bash
curl -fsSL https://raw.githubusercontent.com/skavgou/spec-convert/main/install.sh | bash
```

**Windows (PowerShell)**
```powershell
irm https://raw.githubusercontent.com/skavgou/spec-convert/main/install.ps1 | iex
```

---

## Usage

```
swf-migrate <input-file> [-o <output-file>]
```

- Input can be `.json`, `.yaml`, or `.yml`
- Output defaults to `<input-stem>-migrated.yaml` alongside the input file
- Use `-o` to specify a custom output path (format inferred from extension)

**Examples**

```bash
# Default output → samples/hello-migrated.yaml
swf-migrate samples/hello.json

# Explicit output path
swf-migrate samples/hello.json -o results/hello-v1.yaml
```

---

## Build from source

Requires Java 17+ and Maven 3.8+.

**Fat jar (requires Java to run)**
```bash
mvn package
java -jar target/spec-convert.jar <input-file> [-o <output-file>]
```

**Native binary (no Java required to run)**

Requires [GraalVM JDK 21](https://www.graalvm.org/downloads/) to build.

```bash
mvn package -Pnative
./target/swf-migrate <input-file> [-o <output-file>]
```

---


## Conversion details

See [`CONVERSION_NOTES.md`](CONVERSION_NOTES.md) for a full description of the field mappings and state-type handling.
