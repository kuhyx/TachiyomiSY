#!/bin/bash

# ============================================================================
# Fail if any file in the commit exceeds the shared 250-line cap.
#
# Thin delegate to the shared gate in ~/src/utils, which owns the cap and the
# exemption list (generated / vendored / data files). Copying that logic here
# is what lets one repo's idea of "too long" drift from every other repo's --
# so this script only locates the shared checker and forwards its arguments.
#
# Usage:
#   scripts/check_file_length.sh <file> [<file> ...]   # pre-commit passes these
#   scripts/check_file_length.sh --all                 # whole tree, from cwd
#
# Files under a Gradle module not yet in CAPPED_MODULES (scripts/capped_modules.sh)
# are skipped, mirroring ci_gates.sh: the cap rolls out one module at a time.
# ============================================================================

set -euo pipefail

readonly SHARED_GATE="${UTILS_ROOT:-$HOME/src/utils}/scripts/check_file_length.sh"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
readonly REPO_ROOT
# shellcheck source=scripts/capped_modules.sh
source "$REPO_ROOT/scripts/capped_modules.sh"

in_scope() {
    # The cap rolls out per module (CAPPED_MODULES); a file under a module not
    # yet on the list is checked by nobody until that module's split session,
    # exactly as ci_gates.sh scopes it. --all and other flags pass through.
    local skip
    skip="$(uncapped_module_pattern)"
    for arg in "$@"; do
        if [[ "$arg" == -* ]] || ! [[ "$arg" =~ $skip ]]; then
            printf '%s\n' "$arg"
        fi
    done
}

main() {
    if [[ ! -x "$SHARED_GATE" ]]; then
        echo "Error: shared file-length gate not found at $SHARED_GATE" >&2
        echo "       Clone github.com/kuhyx/utils to ~/src/utils, or set" >&2
        echo "       UTILS_ROOT to where it lives." >&2
        exit 1
    fi

    local -a scoped=()
    mapfile -t scoped < <(in_scope "$@")
    if [[ ${#scoped[@]} -eq 0 ]]; then
        echo "file length: every given file is under a module not yet on the cap"
        exit 0
    fi
    exec bash "$SHARED_GATE" "${scoped[@]}"
}

main "$@"
