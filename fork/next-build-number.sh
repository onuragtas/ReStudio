#!/usr/bin/env bash
# Prints the build number for the next ReStudio build: <branch>.<build of the upstream base>.<counter>.
# The IDE shows an update only for a higher build number, so the counter is always higher than the counter of the
# published release (updates.xml on the update-metadata release). In CI, the counter is at least 1000 + the run
# number. A build without a number is a SNAPSHOT build, and the IDE never shows an update for it.
#
# Usage: fork/next-build-number.sh [<repository>]   (the default repository is onuragtas/ReStudio)
set -euo pipefail

repository="${1:-onuragtas/ReStudio}"
root="$(cd "$(dirname "$0")/.." && pwd)"
tag="$(tr -d '[:space:]' < "$root/fork/UPSTREAM_BASE")"
prefix="$(cut -d. -f1-2 <<< "${tag#idea/}")"

counter=1001
if [ -n "${GITHUB_RUN_NUMBER:-}" ]; then
  counter=$((1000 + GITHUB_RUN_NUMBER))
fi

published="$(gh release download update-metadata --repo "$repository" --pattern updates.xml --output - 2>/dev/null \
  | sed -n 's/.*<build number="\([0-9.]*\)".*/\1/p' | head -n 1 || true)"
if [ "${published%.*}" = "$prefix" ] && [ $((${published##*.} + 1)) -gt "$counter" ]; then
  counter=$((${published##*.} + 1))
fi

echo "$prefix.$counter"
