#!/bin/bash

# ============================================================================
# Install this repo's git hooks.
#
# .git/hooks/ is not tracked, so a fresh clone has no hooks. Run this once
# after cloning. Both stages come from .pre-commit-config.yaml: the read-only
# gates at commit time, and scripts/ci_gates.sh at push time.
# ============================================================================

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
readonly REPO_ROOT

main() {
    if ! command -v pre-commit >/dev/null 2>&1; then
        echo "Error: pre-commit is not installed (pacman -S pre-commit)" >&2
        exit 1
    fi
    pre-commit install --config "$REPO_ROOT/.pre-commit-config.yaml" \
        --hook-type pre-commit --hook-type pre-push
    echo "Hooks installed: pre-commit (gates) + pre-push (CI mirror)."
}

main "$@"
