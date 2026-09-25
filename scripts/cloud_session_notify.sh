#!/bin/bash
# ============================================================================
# Notification hook: a cloud session that needs permission, or that has gone
# idle waiting for input, says so on the status-feed issue. A local session can
# then see the stall without reading the cloud transcript.
#
# Reads the hook's JSON payload on stdin. No-op outside a cloud session, so a
# local run never posts. Never fails the session: every path exits 0.
# ============================================================================
set -uo pipefail
readonly REPO="kuhyx/TachiyomiSY"
readonly FEED_ISSUE=25
readonly CLOUD_HOME="/home/user"

[[ "$HOME" == "$CLOUD_HOME" ]] || exit 0
command -v gh >/dev/null 2>&1 || exit 0

payload=$(cat 2>/dev/null || true)
message=$(printf '%s' "$payload" | python3 -c '
import json,sys
try:
    print(json.load(sys.stdin).get("message", "")[:400])
except Exception:
    print("")
' 2>/dev/null)
[[ -n "${message// }" ]] || exit 0

branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "unknown")
gh issue comment "$FEED_ISSUE" -R "$REPO" \
    --body "\`$branch\` at $(date -u +%H:%MZ): $message" >/dev/null 2>&1 || true
exit 0
