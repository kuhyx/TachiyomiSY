#!/bin/bash

# ============================================================================
# Which modules' tests a push has to run. Sourced by ci_gates.sh, not executed.
# Expects REPO_ROOT, BUILD_INPUTS and push_range from the caller and
# module_dirs from capped_modules.sh.
# ============================================================================

# The Gradle project paths (`:core:common`) of the modules the push range touches,
# comma-separated, or "" when the range also touches something every module
# depends on (root build files, the version catalogs, build-logic, detekt
# config) and the whole tree must run. Tests and coverage of modules outside
# the list are skipped through the `mihon.gate.modules` property that the
# convention plugins read; compilation is never narrowed, so a consumer of a
# changed module still has to build.
gate_modules() {
    local range file dir module modules=()
    range="$(push_range)"
    while IFS= read -r file; do
        [[ -z "$file" ]] && continue
        module=""
        for dir in $(module_dirs); do
            if [[ "$file" == "$dir"/* && "$dir" != gradle/build-logic ]]; then
                module="$dir"
                break
            fi
        done
        if [[ -z "$module" ]]; then
            return 0
        fi
        [[ " ${modules[*]:-} " == *" $module "* ]] || modules+=("$module")
    done < <(git -C "$REPO_ROOT" diff --name-only "$range" -- "${BUILD_INPUTS[@]}" 2>/dev/null || true)
    if [[ ${#modules[@]} -gt 0 ]]; then
        printf ':%s\n' "${modules[@]//\//:}" | paste -sd, -
    fi
}

