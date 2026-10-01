#!/usr/bin/env bash
# Builds user-facing release notes for the GitHub Release page and followers' feeds.
# The feed only shows the title and the first lines, so the notes open with a short
# "at a glance" block (what's new, what's fixed, security, why it matters, upgrade impact).
#
# Usage:   release-notes.sh <version> <previous-tag or ""> <owner/repo>
# Outputs: release-notes.md, release-title.txt, CHANGELOG.md ([Unreleased] stamped as [version])
#
# Content sources (best first):
#   1. "## [<version>]" or "## [Unreleased]" section of CHANGELOG.md (written by a human)
#   2. Conventional Commit messages since the previous tag (feat:, fix:, security:, docs: ...)
set -euo pipefail

VERSION="$1"; PREV="${2:-}"; REPO="$3"
OWNER="${REPO%%/*}"; NAME="${REPO##*/}"
DATE=$(date -u +%Y-%m-%d)
TEMPLATE=".github/CHANGELOG_TEMPLATE.md"

section() {   # body of "## [name]" in CHANGELOG.md
  [ -f CHANGELOG.md ] || return 0
  awk -v h="## [$1]" 'index($0,h)==1{f=1;next} f&&/^## \[/{exit} f{print}' CHANGELOG.md
}
clean() {     # drop HTML comments and headings without content
  awk '
    function flush() { if (h != "" && buf != "") printf "%s\n%s\n", h, buf }
    /^[[:space:]]*<!--/ { next }
    /^### /             { flush(); h=$0; buf=""; next }
                        { if (h == "") { if (NF) print; next } if (NF) buf = buf $0 "\n" }
    END                 { flush() }'
}
has_text() { grep -q '[^[:space:]]' <<<"$1"; }
first_in() {  # first item under a "### <pattern>" heading, without markdown/commit link
  awk -v h="$1" '/^### /{f=($0 ~ h); next} f && NF {sub(/^[-*][[:space:]]*/,""); print; exit}' <<<"$2" \
    | sed -e 's/\*\*//g' -e 's/ (\[`[0-9a-f]*`\](.*))$//'
}
count_in() {  # number of bullet items under a "### <pattern>" heading
  awk -v h="$1" '/^### /{f=($0 ~ h); next} f && /^[-*] /{n++} END{print n+0}' <<<"$2"
}

# ---------- 1. curated notes ----------
CURATED=$(section "$VERSION" | clean); STAMP=0
if ! has_text "$CURATED"; then CURATED=$(section Unreleased | clean); STAMP=1; fi

# ---------- 2. commits since previous release ----------
RANGE="HEAD"; [ -n "$PREV" ] && RANGE="$PREV..HEAD"
FEAT=""; FIX=""; SEC=""; IMP=""; DOC=""; MAINT=""; OTHER=""; BREAKING=""; ALL=""; N=0
SEC_RE='(security|cve-|ghsa-|vulnerab)'
shopt -s nocasematch
while IFS=$'\t' read -r h s; do
  [ -z "${h:-}" ] && continue
  [[ "$s" == *"[skip ci]"* ]] && continue
  msg="$s"; type=""
  if [[ "$s" =~ ^([a-z]+)(\([^\)]*\))?(!)?:[[:space:]]*(.*)$ ]]; then
    type="${BASH_REMATCH[1],,}"; msg="${BASH_REMATCH[4]}"
    [ -n "${BASH_REMATCH[3]}" ] && BREAKING+="- ${msg^}"$'\n'
  fi
  [[ "$s" == *"BREAKING"* ]] && BREAKING+="- ${msg^}"$'\n'
  line="- ${msg^} ([\`$h\`](https://github.com/$REPO/commit/$h))"$'\n'
  ALL+="$line"; N=$((N + 1))
  if [[ "$type" == sec* || "$s" =~ $SEC_RE ]]; then SEC+="$line"
  else
    case "$type" in
      feat)                       FEAT+="$line" ;;
      fix)                        FIX+="$line" ;;
      perf|refactor)              IMP+="$line" ;;
      docs)                       DOC+="$line" ;;
      ci|build|chore|test|style)  MAINT+="$line" ;;
      *)                          OTHER+="$line" ;;
    esac
  fi
done < <(git log --no-merges -n 300 --format='%h%x09%s' $RANGE)
shopt -u nocasematch

generated() {
  [ -n "$BREAKING" ] && printf '### ⚠️ Breaking changes\n%s\n' "$BREAKING"
  [ -n "$FEAT" ]     && printf '### ✨ Added\n%s\n' "$FEAT"
  [ -n "$FIX" ]      && printf '### 🐞 Fixed\n%s\n' "$FIX"
  [ -n "$SEC" ]      && printf '### 🔒 Security\n%s\n' "$SEC"
  [ -n "$IMP" ]      && printf '### ⚡ Improvements\n%s\n' "$IMP"
  [ -n "$DOC" ]      && printf '### 📚 Documentation\n%s\n' "$DOC"
  [ -n "$OTHER" ]    && printf '### Other changes\n%s\n' "$OTHER"
  [ -n "$MAINT" ]    && printf '### 🔧 Maintenance\n%s\n' "$MAINT"
  return 0
}

# ---------- 3. body, summary and upgrade impact ----------
if has_text "$CURATED"; then
  BODY=$(awk '/^### Overview/{s=1;next} s&&/^### /{s=0} !s' <<<"$CURATED")
  OVERVIEW=$(awk '/^### Overview/{f=1;next} f&&/^### /{exit} f&&NF{sub(/^[-*>][[:space:]]*/,""); print; exit}' <<<"$CURATED")
else
  BODY=$(generated)
  OVERVIEW=""
fi
[ -z "$OVERVIEW" ] && OVERVIEW=$(first_in 'Added' "$BODY")
[ -z "$OVERVIEW" ] && OVERVIEW=$(first_in 'Fixed' "$BODY")
[ -z "$OVERVIEW" ] && OVERVIEW=$(first_in 'Security' "$BODY")
[ -z "$OVERVIEW" ] && OVERVIEW="Maintenance and stability improvements"
OVERVIEW=$(sed 's/\*\*//g' <<<"$OVERVIEW")

if grep -qiE '^### .*(Breaking|Removed)' <<<"$BODY"; then
  IMPACT="⚠️ **Action required** – contains breaking changes, read *Breaking* before upgrading"
elif grep -qiE '^### .*Security' <<<"$BODY"; then
  IMPACT="🔒 **Recommended** – contains security fixes, no code changes needed"
elif grep -qiE '^### .*Fixed' <<<"$BODY"; then
  IMPACT="🐞 **Safe** – bug fixes only, no code changes needed"
else
  IMPACT="✅ **Drop-in** – no code changes needed"
fi

glance() {    # "- <icon> **<label>:** first item (+N more)"
  local icon="$1" label="$2" pat="$3" first n more=""
  first=$(first_in "$pat" "$BODY"); [ -z "$first" ] && return 0
  n=$(count_in "$pat" "$BODY"); [ "$n" -gt 1 ] && more=" *(+$((n - 1)) more)*"
  printf -- '- %s **%s:** %s%s\n' "$icon" "$label" "$first" "$more"
}

# ---------- 4. release-notes.md ----------
{
  echo "**${OVERVIEW}**"
  echo
  echo "#### At a glance"
  glance "⚠️" "Breaking"       'Breaking|Removed'
  glance "🆕" "What's new"     'Added'
  glance "🐞" "Fixed"          'Fixed'
  glance "🔒" "Security"       'Security'
  glance "⚡" "Improved"       'Changed|Improvements'
  glance "💡" "Why it matters" 'Why it matters'
  echo "- ⬆️ **Upgrade:** $IMPACT"
  echo
  echo "---"
  echo
  printf '%s\n\n' "$BODY"
  if has_text "$CURATED" && [ "$N" -gt 0 ]; then
    printf '<details><summary>All commits in this release (%s)</summary>\n\n%s\n</details>\n\n' "$N" "$ALL"
  fi
  echo "### 📦 Install"
  echo
  echo '```xml'
  echo '<repository><id>jitpack.io</id><url>https://jitpack.io</url></repository>'
  echo
  echo "<dependency>"
  echo "  <groupId>com.github.$OWNER</groupId>"
  echo "  <artifactId>$NAME</artifactId>"
  echo "  <version>$VERSION</version>"
  echo "</dependency>"
  echo '```'
  echo
  echo "Gradle: \`implementation 'com.github.$OWNER:$NAME:$VERSION'\`"
  echo
  if [ -n "$PREV" ]; then
    echo "**Full changelog:** [\`$PREV...$VERSION\`](https://github.com/$REPO/compare/$PREV...$VERSION)"
  else
    echo "**Full changelog:** [commits up to \`$VERSION\`](https://github.com/$REPO/commits/$VERSION)"
  fi
} > release-notes.md

printf '%s %s — %s' "$NAME" "$VERSION" "${OVERVIEW:0:90}" > release-title.txt

# ---------- 5. stamp CHANGELOG.md ([Unreleased] -> [version] - date, fresh template on top) ----------
if [ "$STAMP" = 1 ] && [ -f CHANGELOG.md ] && grep -q '^## \[Unreleased\]' CHANGELOG.md; then
  { echo "### Overview"; echo "$OVERVIEW"; echo; printf '%s\n' "$BODY"; } > .section.tmp
  awk -v v="$VERSION" -v d="$DATE" -v tpl="$TEMPLATE" -v sec=".section.tmp" '
    index($0,"## [Unreleased]")==1 && !done {
      print; print ""
      while ((getline l < tpl) > 0) print l
      print ""; print "## [" v "] - " d; print ""
      while ((getline l < sec) > 0) print l
      print ""; done=1; skip=1; next }
    skip && /^## \[/ { skip=0 }
    !skip { print }' CHANGELOG.md > CHANGELOG.tmp
  mv CHANGELOG.tmp CHANGELOG.md
  rm -f .section.tmp
fi

echo "Title: $(cat release-title.txt)"
echo "----- release-notes.md -----"
cat release-notes.md
