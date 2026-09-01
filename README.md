# swf-migrate

CNCF Serverless Workflow **0.8 → 1.0** converter.

---

## Install (no Java required)

Download a pre-built binary from the [releases page](../../releases) and place it on your PATH, or use the one-liner for your platform.

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
swf-migrate <input-file> [-o <output-file>] [-f yaml|json] [-n <namespace>]
```

| Flag | Description | Default |
|------|-------------|---------|
| `-o`, `--output` | Output file path (format inferred from extension) | `<input-stem>-migrated.yaml` |
| `-f`, `--format` | Output format: `yaml` or `json` | `yaml` |
| `-n`, `--namespace` | Namespace written to the 1.0 document header | `default` |

- Input can be `.json`, `.yaml`, or `.yml`

**Examples**

```bash
# Default output → samples/hello-migrated.yaml
swf-migrate samples/hello.json

# Explicit output path
swf-migrate samples/hello.json -o results/hello-v1.yaml

# Output as JSON
swf-migrate samples/hello.json -f json

# Custom output path and namespace
swf-migrate samples/hello.json -o results/hello-v1.yaml -n my-org
```

---

## Build from source

Requires Java 17+ and Maven 3.8+.

**Fat jar (requires Java to run)**
```bash
mvn package
java -jar target/spec-convert.jar <input-file> [-o <output-file>] [-f yaml|json] [-n <namespace>]
```

**Native binary (no Java required to run)**

Requires [GraalVM JDK 21](https://www.graalvm.org/downloads/) to build.

```bash
mvn package -Pnative
./target/swf-migrate <input-file> [-o <output-file>] [-f yaml|json] [-n <namespace>]
```

---


## Conversion details

See [`CONVERSION_NOTES.md`](CONVERSION_NOTES.md) for a full description of the field mappings and state-type handling.
