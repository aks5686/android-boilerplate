#!/usr/bin/env bash
#
# setup.sh — rename this Android Clean Architecture boilerplate to a new app.
#
# Usage: ./setup.sh MyApp
#
# Renames all occurrences of "Boilerplate" / "boilerplate" throughout the
# project (applicationId, package folders, Kotlin package declarations and
# imports, app name, rootProject.name, CI references) to the given app name.
#
# Does NOT run a gradle build, touch README.md, or modify .gitignore.

set -euo pipefail

usage() {
    echo "Usage: $0 <NewAppName>"
    echo "Example: $0 MyApp"
    exit 1
}

if [[ $# -lt 1 || -z "${1:-}" ]]; then
    usage
fi

NEW_NAME_RAW="$1"

# Only allow alphanumeric app names (keeps package/identifier rules valid).
if [[ ! "$NEW_NAME_RAW" =~ ^[A-Za-z][A-Za-z0-9]*$ ]]; then
    echo "Error: app name must start with a letter and contain only letters and digits." >&2
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

OLD_PASCAL="Boilerplate"
OLD_LOWER="boilerplate"

NEW_PASCAL="${NEW_NAME_RAW}"
NEW_LOWER="$(echo "$NEW_NAME_RAW" | tr '[:upper:]' '[:lower:]')"

OLD_PACKAGE="com.aks.${OLD_LOWER}"
NEW_PACKAGE="com.aks.${NEW_LOWER}"

echo "Renaming project: ${OLD_PASCAL} -> ${NEW_PASCAL}"
echo "Package: ${OLD_PACKAGE} -> ${NEW_PACKAGE}"

# 1. Move package folder structure under app/src/*/java
for SRC_ROOT in app/src/main/java app/src/test/java app/src/androidTest/java; do
    OLD_DIR="${SRC_ROOT}/com/aks/${OLD_LOWER}"
    NEW_DIR="${SRC_ROOT}/com/aks/${NEW_LOWER}"
    if [[ -d "$OLD_DIR" ]]; then
        mkdir -p "$(dirname "$NEW_DIR")"
        git mv "$OLD_DIR" "$NEW_DIR" 2>/dev/null || mv "$OLD_DIR" "$NEW_DIR"
    fi
done

# 2. Rename file contents: package declarations, imports, class name references, strings.
#    Order matters: replace the longer/more specific token first.
TEXT_FILES=$(grep -rlI \
    -e "$OLD_PACKAGE" \
    -e "$OLD_PASCAL" \
    -e "$OLD_LOWER" \
    . \
    --exclude-dir=.git \
    --exclude-dir=.gradle \
    --exclude-dir=.idea \
    --exclude-dir=build \
    --exclude=setup.sh \
    --exclude=README.md \
    --exclude=.gitignore \
    2>/dev/null || true)

if [[ -n "$TEXT_FILES" ]]; then
    while IFS= read -r FILE; do
        [[ -f "$FILE" ]] || continue
        sed -i.bak \
            -e "s/${OLD_PACKAGE}/${NEW_PACKAGE}/g" \
            -e "s/${OLD_PASCAL}/${NEW_PASCAL}/g" \
            -e "s/${OLD_LOWER}/${NEW_LOWER}/g" \
            "$FILE"
        rm -f "${FILE}.bak"
    done <<< "$TEXT_FILES"
fi

# 3. Rename any remaining files whose *name* contains "Boilerplate" (e.g. BoilerplateApplication.kt).
find . \
    \( -path ./.git -o -path ./.gradle -o -path ./.idea -o -name build \) -prune -o \
    -type f -name "*${OLD_PASCAL}*" -print | while IFS= read -r FILE; do
    DIR="$(dirname "$FILE")"
    BASE="$(basename "$FILE")"
    NEW_BASE="${BASE//${OLD_PASCAL}/${NEW_PASCAL}}"
    if [[ "$BASE" != "$NEW_BASE" ]]; then
        git mv "$FILE" "${DIR}/${NEW_BASE}" 2>/dev/null || mv "$FILE" "${DIR}/${NEW_BASE}"
    fi
done

chmod +x "$SCRIPT_DIR/setup.sh"

echo "Done. Project renamed to ${NEW_PASCAL} (package ${NEW_PACKAGE})."
