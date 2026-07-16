# Compiles SpecConvert and packages it into out/spec-convert.jar
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC="$SCRIPT_DIR/src/main/java"
OUT="$SCRIPT_DIR/out"
LIB="$SCRIPT_DIR/lib"

# Build classpath from every jar in lib/
CP="$(find "$LIB" -name '*.jar' | tr '\n' ':')"
CP="${CP%:}"  # strip trailing colon

echo "Compiling..."
mkdir -p "$OUT/classes"
javac -cp "$CP" -d "$OUT/classes" "$SRC/com/specconvert/SpecConvert.java"

echo "Extracting dependencies..."
EXTRACT_DIR="$OUT/uber"
rm -rf "$EXTRACT_DIR"
mkdir -p "$EXTRACT_DIR"
# Unpack each dependency JAR
for jar in "$LIB"/*.jar; do
    (cd "$EXTRACT_DIR" && jar xf "$jar")
done
# Overlay compiled classes on top
cp -r "$OUT/classes/." "$EXTRACT_DIR/"

echo "Packaging fat jar..."
jar --create --file "$OUT/spec-convert.jar" \
    --main-class com.specconvert.SpecConvert \
    -C "$EXTRACT_DIR" .

# Clean up staging dirs
rm -rf "$OUT/classes" "$EXTRACT_DIR"

echo "Build complete → out/spec-convert.jar  (self-contained, no lib/ needed)"
