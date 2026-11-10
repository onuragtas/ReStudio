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
    # The second column is a Marketplace update ID, or the URL of a zip, for example a ReStudio-langs release.
    if [[ "$update" == https://* ]]; then url="$update"; else url="https://plugins.jetbrains.com/plugin/download?updateId=$update"; fi
    curl -sSfL --retry 5 -o "$zip.part" "$url"
    mv "$zip.part" "$zip"
  fi
  if ! echo "$sha256  $zip" | shasum -a 256 -c --status; then
    echo "The SHA-256 of $id $version is not $sha256" >&2
    exit 1
  fi
  unzip -q "$zip" -d "$target/$mode"
  # LSP4IJ copies its TextMate grammar to <plugin>/bundles/<version> at the first start, if it is not there.
  # The signed macOS app is read-only, so the copy fails and breaks the signature. The build puts the copy there.
  if [[ "$id" == "com.redhat.devtools.lsp4ij" ]]; then
    bundles="$target/$mode/lsp4ij/bundles/$version"
    mkdir -p "$bundles"
    for jar in "$target/$mode"/lsp4ij/lib/*.jar; do
      unzip -q -o "$jar" 'textmate/*' -d "$bundles.tmp" 2> /dev/null || true
    done
    cp -R "$bundles.tmp/textmate/." "$bundles/"
    rm -rf "$bundles.tmp"
  fi
  echo "$id $version ($license, $mode)"
done < "$list"

ls "$target"/*
