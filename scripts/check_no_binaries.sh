#!/bin/bash

# ============================================================================
# Fail if any file in the commit is a binary/image outside .binary-allowlist.
#
# Thin delegate to the shared gate in ~/src/utils, which owns the extension
# list; .binary-allowlist in this repo holds the launcher icons and wrapper
# jar the build genuinely needs. Copying the logic here is what lets one
# repo's idea of "binary" drift from every other repo's.
#
# Usage:
#   scripts/check_no_binaries.sh <file> [<file> ...]   # pre-commit passes these
# ============================================================================

set -euo pipefail

readonly SHARED_GATE="${UTILS_ROOT:-$HOME/src/utils}/scripts/check_no_binaries.sh"

main() {
    if [[ ! -x "$SHARED_GATE" ]]; then
        echo "Error: shared no-binaries gate not found at $SHARED_GATE" >&2
        echo "       Clone github.com/kuhyx/utils to ~/src/utils, or set" >&2
        echo "       UTILS_ROOT to where it lives." >&2
        exit 1
    fi

    exec bash "$SHARED_GATE" "$@"
}

main "$@"
