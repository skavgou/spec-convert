# Compiles SpecConvert and packages it into target/spec-convert.jar
# Requires Maven and the io.serverlessworkflow.v08 artifact to be installed locally.
# To install the v08 shaded jar, see README.md.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "Building with Maven..."
mvn -f "$SCRIPT_DIR/pom.xml" package -q

echo "Build complete → target/spec-convert.jar  (self-contained, no lib/ needed)"
