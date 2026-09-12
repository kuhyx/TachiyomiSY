#!/bin/bash

# ============================================================================
# Turn an upstream-sync outcome into exactly one open issue, or none.
#
# Reads the env the workflow sets from scripts/upstream_sync.sh's outputs:
#   STATUS        current | pushed | conflict | red | (empty = crashed)
#   CONFLICTS     space-separated conflicting paths (status=conflict)
#   UPSTREAM_SHA  the upstream tip the run tried to land on
#   RUN_URL       link to the Actions run, whose log has the details
#
# Green closes any open `upstream-sync` issue; red or conflict creates one or
# appends to the one already open, so a week of failures is one thread.
# ============================================================================

set -euo pipefail

readonly LABEL="upstream-sync"
STATUS="${STATUS:-crashed}"
CONFLICTS="${CONFLICTS:-}"
UPSTREAM_SHA="${UPSTREAM_SHA:-unknown}"
RUN_URL="${RUN_URL:-}"

ensure_label() {
    gh label create "$LABEL" --color D93F0B \
        --description "daily rebase onto jobobby04/TachiyomiSY needs a hand" \
        2>/dev/null || true
}

repo_slug() {
    # Actions sets GITHUB_REPOSITORY. Locally the slug comes from the origin
    # URL, never from gh's own resolution: with an `upstream` remote present
    # gh targets THAT repo by default, and this script would have filed its
    # issues against jobobby04/TachiyomiSY (caught in testing by a missing
    # label, not by design).
    if [[ -n "${GITHUB_REPOSITORY:-}" ]]; then
        echo "$GITHUB_REPOSITORY"
    else
        git remote get-url origin | sed -E 's#.*github\.com[:/]##; s#\.git$##'
    fi
}

open_issue_number() {
    # REST, not `gh issue list`: the list endpoint is search-backed and lags
    # a freshly created issue by seconds, which opened a duplicate in testing.
    gh api "repos/$(repo_slug)/issues?labels=$LABEL&state=open&per_page=1" \
        --jq '.[0].number // empty'
}

body_for() {
    # Markdown code spans; the backtick goes through a variable so the
    # format strings stay plain.
    local today tick='`'
    printf -v today '%(%Y-%m-%d)T' -1
    case "$STATUS" in
        conflict)
            printf '%s: rebase onto upstream %s%s%s conflicts in:\n\n' \
                "$today" "$tick" "$UPSTREAM_SHA" "$tick"
            local path
            for path in $CONFLICTS; do
                printf -- '- %s%s%s\n' "$tick" "$path" "$tick"
            done
            ;;
        red)
            printf '%s: rebased cleanly onto upstream %s%s%s, but a gate is red. Nothing was pushed.\n' \
                "$today" "$tick" "$UPSTREAM_SHA" "$tick"
            ;;
        *)
            printf '%s: the sync job crashed before reporting a status (%s).\n' "$today" "$STATUS"
            ;;
    esac
    printf '\nLog: %s\n\nResolve in a session on %s~/src/tachiyomisy%s: %sscripts/upstream_sync.sh --dry-run%s reproduces it.\n' \
        "$RUN_URL" "$tick" "$tick" "$tick" "$tick"
}

report_failure() {
    ensure_label
    local existing body
    existing="$(open_issue_number)"
    body="$(body_for)"
    if [[ -n "$existing" ]]; then
        gh issue comment "$existing" --body "$body"
        echo "Updated issue #$existing (status=$STATUS)"
    else
        gh issue create --label "$LABEL" \
            --title "upstream-sync: $STATUS on $(date +%Y-%m-%d)" \
            --body "$body"
        echo "Opened a new issue (status=$STATUS)"
    fi
}

close_open_issue() {
    local existing
    existing="$(open_issue_number)"
    if [[ -n "$existing" ]]; then
        gh issue close "$existing" \
            --comment "Resolved: master is on top of upstream \`$UPSTREAM_SHA\` (status=$STATUS)."
        echo "Closed issue #$existing"
    fi
}

main() {
    GH_REPO="$(repo_slug)"
    export GH_REPO
    echo "Reporting to $GH_REPO"
    case "$STATUS" in
        current|pushed) close_open_issue ;;
        *) report_failure ;;
    esac
}

main "$@"
