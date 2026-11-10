#!/usr/bin/env bash
# Merges the patch directories of the release workflow into one directory for fork/publish-release.sh.
# Each job of the workflow runs fork/make-patches.sh for its own installers and uploads a patches-* artifact.
#
# Output: the patches in <out>, and <out>/patches.txt in the format of fork/make-patches.sh. Without a patches.txt in
# <in>, the output has no patches.txt. Then the release has no patches.
#
# Usage: fork/merge-patches.sh <in> <out>
set -euo pipefail

in="$1"
out="$2"
mkdir -p "$out"
shopt -s nullglob

lists=("$in"/*/patches.txt)
if [ ${#lists[@]} -eq 0 ]; then
  echo "No patches.txt in $in. The release has no patches."
  exit 0
fi

old_build="$(head -n 1 "${lists[0]}")"
echo "$old_build" > "$out/patches.txt"
for list in "${lists[@]}"; do
  if [ "$(head -n 1 "$list")" != "$old_build" ]; then
    echo "$list is for the old build $(head -n 1 "$list"), not $old_build" >&2
    exit 1
  fi
  tail -n +2 "$list" >> "$out/patches.txt"
done
for jar in "$in"/*/*.jar; do
  cp "$jar" "$out/"
done
cat "$out/patches.txt"
