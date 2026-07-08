#!/usr/bin/env bash
# Sync ISSUES.md -> GitHub Issues
# Parses openspec/changes/makmur-0/ISSUES.md and creates/updates GitHub Issues.
# Usage: GITHUB_TOKEN=<token> ./sync-md-to-issues.sh
# Run from repo root.

set -euo pipefail

REPO="orbit2eyes/makmur"
ISSUES_FILE="openspec/changes/makmur-0/ISSUES.md"

if [ ! -f "$ISSUES_FILE" ]; then
  echo "ERROR: $ISSUES_FILE not found. Run from repo root."
  exit 1
fi

echo "Parsing $ISSUES_FILE..."

# Fetch existing GitHub issues (open + closed)
gh issue list --repo "$REPO" --state all --json number,title,state,body --limit 100 > /tmp/gh-issues.json

# Parse ISSUES.md: extract entries from "###" headings
# Entry pattern: `### [ID]: [Title]` or `### #[num]: [Title]`
entries=$(grep -n '^### ' "$ISSUES_FILE" || true)

if [ -z "$entries" ]; then
  echo "No issues found in ISSUES.md"
  exit 0
fi

created=0
updated=0
skipped=0

echo "$entries" | while read -r line_info; do
  line_num=$(echo "$line_info" | cut -d: -f1)
  heading=$(echo "$line_info" | sed 's/^[0-9]*://' | sed 's/^### //')

  # Extract title and GitHub issue number from heading
  gh_num=$(echo "$heading" | grep -oP '\(#[0-9]+\)' | grep -oP '[0-9]+' || echo "")
  title=$(echo "$heading" | sed 's/^#[0-9]*: //' | sed 's/^I-[0-9]*: //' | sed 's/ (#[0-9]*)//' | sed 's/ (#resolved)//' | xargs)

  # Extract body from ISSUES.md (content after heading until next heading or end of section)
  body_start=$((line_num + 1))
  # Find next ### or end of file
  next_line=$(tail -n +"$body_start" "$ISSUES_FILE" | grep -n '^### ' | head -1 | cut -d: -f1 || echo "")
  if [ -n "$next_line" ]; then
    body_end=$((line_num + next_line - 1))
  else
    body_end=$(wc -l < "$ISSUES_FILE")
  fi

  # Extract body text
  body=$(sed -n "${body_start},${body_end}p" "$ISSUES_FILE" | \
    sed '/^---$/q' | \
    sed '/^## /,$d' | \
    sed '/^$/d' | \
    head -30 || true)

  # Truncate body to first non-empty 20 lines
  body=$(echo "$body" | head -20)

  # Skip empty titles (separators, section headers)
  if [ -z "$title" ] || [ "$title" = "Open Issues" ] || [ "$title" = "Closed Issues" ] || [ "$title" = "Resolved Issues" ]; then
    skipped=$((skipped + 1))
    continue
  fi

  # Check if issue with this title already exists on GitHub
  existing=$(jq -r --arg t "$title" '.[] | select(.title == $t) | .number' /tmp/gh-issues.json | head -1)

  if [ -n "$existing" ]; then
    # Update existing issue body
    current_body=$(jq -r --arg t "$title" '.[] | select(.title == $t) | .body // ""' /tmp/gh-issues.json | head -1)
    if [ "$current_body" != "$body" ]; then
      gh issue edit "$existing" --repo "$REPO" --body "$body" --title "$title" 2>/dev/null
      echo "Updated #$existing: $title"
      updated=$((updated + 1))
    else
      skipped=$((skipped + 1))
    fi
  else
    # Create new issue
    gh issue create --repo "$REPO" --title "$title" --body "$body" 2>/dev/null
    echo "Created issue: $title"
    created=$((created + 1))
  fi
done

echo ""
echo "Done: $created created, $updated updated, $skipped skipped."
