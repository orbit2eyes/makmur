#!/usr/bin/env bash
# Sync GitHub Issues -> openspec/specs/ISSUES.md
# Usage: GITHUB_TOKEN=<token> ./sync-issues.sh
# Run from repo root.

set -euo pipefail

REPO="orbit2eyes/makmur"
ISSUES_FILE="openspec/specs/ISSUES.md"

echo "Fetching issues from $REPO..."

# Fetch all open issues (state=open)
gh issue list --repo "$REPO" --state open --json number,title,labels,body,createdAt,updatedAt --limit 100 > /tmp/issues-open.json

# Fetch all closed issues (last 50)
gh issue list --repo "$REPO" --state closed --json number,title,labels,body,createdAt,closedAt --limit 50 > /tmp/issues-closed.json

echo "Generating $ISSUES_FILE..."

cat > "$ISSUES_FILE" << 'HEADER'
# Issues — Makmur

> Sync target: GitHub Issues (repo: orbit2eyes/makmur)
> Auto-synced bidirectionally: GitHub Issues <-> ISSUES.md

HEADER

# --- Open Issues ---
echo "" >> "$ISSUES_FILE"
echo "## Open Issues" >> "$ISSUES_FILE"
echo "" >> "$ISSUES_FILE"

jq -c '.[]' /tmp/issues-open.json | while read -r issue; do
  num=$(echo "$issue" | jq -r '.number')
  title=$(echo "$issue" | jq -r '.title')
  body=$(echo "$issue" | jq -r '.body // "No description."')
  created=$(echo "$issue" | jq -r '.createdAt')
  updated=$(echo "$issue" | jq -r '.updatedAt')
  labels=$(echo "$issue" | jq -r '[.labels[].name] | join(", ")')

  echo "### #$num: $title" >> "$ISSUES_FILE"
  echo "" >> "$ISSUES_FILE"
  [ -n "$labels" ] && echo "- **Labels**: $labels" >> "$ISSUES_FILE"
  echo "- **Created**: $created" >> "$ISSUES_FILE"
  echo "- **Updated**: $updated" >> "$ISSUES_FILE"
  echo "" >> "$ISSUES_FILE"
  echo "$body" >> "$ISSUES_FILE"
  echo "" >> "$ISSUES_FILE"
  echo "---" >> "$ISSUES_FILE"
  echo "" >> "$ISSUES_FILE"
done

# --- Closed Issues ---
echo "## Closed / Resolved Issues" >> "$ISSUES_FILE"
echo "" >> "$ISSUES_FILE"

jq -c '.[]' /tmp/issues-closed.json | while read -r issue; do
  num=$(echo "$issue" | jq -r '.number')
  title=$(echo "$issue" | jq -r '.title')
  body=$(echo "$issue" | jq -r '.body // "No description."')
  created=$(echo "$issue" | jq -r '.createdAt')
  closed=$(echo "$issue" | jq -r '.closedAt // "unknown"')
  labels=$(echo "$issue" | jq -r '[.labels[].name] | join(", ")')

  echo "### #$num: $title" >> "$ISSUES_FILE"
  echo "" >> "$ISSUES_FILE"
  echo "- **Status**: resolved" >> "$ISSUES_FILE"
  echo "- **Closed**: $closed" >> "$ISSUES_FILE"
  [ -n "$labels" ] && echo "- **Labels**: $labels" >> "$ISSUES_FILE"
  echo "- **Created**: $created" >> "$ISSUES_FILE"
  echo "" >> "$ISSUES_FILE"
  echo "$body" >> "$ISSUES_FILE"
  echo "" >> "$ISSUES_FILE"
  echo "---" >> "$ISSUES_FILE"
  echo "" >> "$ISSUES_FILE"
done

echo "Done. $ISSUES_FILE updated."
