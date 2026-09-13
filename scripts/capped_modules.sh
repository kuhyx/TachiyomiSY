#!/bin/bash

# ============================================================================
# The 250-line cap's rollout scope, shared by CI (ci_gates.sh) and the
# pre-commit hook (check_file_length.sh) so the two can never disagree about
# which files are under the cap yet.
#
# Sourced, not executed. Expects REPO_ROOT to be set by the caller.
# ============================================================================

#: Gradle modules already brought under the 250-line cap. The cap is enforced
#: on everything outside the module directories plus these; a module is
#: appended in the same commit that makes it clean, and the list only grows.
#: Rollout order (AGENTS.md): gradle/build-logic source-api core-metadata
#: core/common domain data presentation-core presentation-widget
#: source-local i18n i18n-sy baseline-profile app.
readonly CAPPED_MODULES=(gradle/build-logic core-metadata)

uncapped_module_pattern() {
    # ERE matching paths under modules NOT yet on the cap, e.g.
    # ^(app|core/common)/ -- a module dir is wherever a build.gradle.kts is.
    local dir name pattern=""
    while IFS= read -r dir; do
        name="${dir#"$REPO_ROOT"/}"
        name="${name%/build.gradle.kts}"
        if [[ " ${CAPPED_MODULES[*]:-} " == *" $name "* ]]; then
            continue
        fi
        pattern="${pattern:+$pattern|}$name"
    done < <(find "$REPO_ROOT" -mindepth 2 -maxdepth 4 -name build.gradle.kts -not -path '*/build/*' | sort)
    echo "^($pattern)/"
}
