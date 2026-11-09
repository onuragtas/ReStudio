#!/usr/bin/env bash
# Downloads the plugins of fork/bundled-plugins.tsv, checks their SHA-256 and unpacks them into
# out/restudio-plugins/<load mode>/. The IDE build reads these directories (ReStudioPlugins in build/src).
#
# Usage: fork/fetch-bundled-plugins.sh
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
list="$root/fork/bundled-plugins.tsv"
cache="$root/out/restudio-plugins-cache"
target="$root/out/restudio-plugins"
mkdir -p "$cache"
rm -rf "$target"
mkdir -p "$target/bundled" "$target/path"

while IFS=$'\t' read -r id update version sha256 license mode; do
  [[ -z "$id" || "$id" == \#* ]] && continue
  if [[ "$mode" != "bundled" && "$mode" != "path" ]]; then
    echo "The load mode of $id is '$mode'. Use 'bundled' or 'path'." >&2
    exit 1
  fi
  zip="$cache/$id-$version.zip"
  if [[ ! -f "$zip" ]] || ! echo "$sha256  $zip" | shasum -a 256 -c --status; then
    echo "Downloading $id $version"
    curl -sSfL --retry 5 -o "$zip.part" "https://plugins.jetbrains.com/plugin/download?updateId=$update"
    mv "$zip.part" "$zip"
  fi
  if ! echo "$sha256  $zip" | shasum -a 256 -c --status; then
    echo "The SHA-256 of $id $version is not $sha256" >&2
    exit 1
  fi
  unzip -q "$zip" -d "$target/$mode"
  echo "$id $version ($license, $mode)"
done < "$list"

ls "$target"/*
