#!/usr/bin/env bash
# Stops with an error if a Linux archive has no path plugins or no Python plugin.
#
# Usage: fork/check-bundled-plugins.sh <archive.tar.gz>...
set -euo pipefail

for archive in "$@"; do
  contents="$(tar -tzf "$archive")"
  for entry in restudio-plugins/Dart/ restudio-plugins/flutter-intellij/ restudio-plugins/lsp4ij/ plugins/python-ce/; do
    if ! grep -q "/$entry" <<< "$contents"; then
      echo "$archive has no $entry" >&2
      exit 1
    fi
  done
  echo "$archive has the bundled plugins"
done
