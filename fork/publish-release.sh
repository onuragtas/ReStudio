#!/usr/bin/env bash
# Publishes the installers as a GitHub release, then publishes updates.xml on the update-metadata release.
# The IDE reads updates.xml from there (ReStudioBranding.UPDATES_URL in build/src).
# The release workflow and the local release (.claude/skills/restudio-release) use this script.
#
# Usage: fork/publish-release.sh <repository> <tag> <commit> <build number> <file>...
#   <tag> is restudio/v<version>, for example restudio/v0.1.0.
set -euo pipefail

repository="$1"
tag="$2"
commit="$3"
build="$4"
shift 4

notes="$(cat <<'NOTES'
Unsigned preview build of ReStudio.

macOS: the app is not signed or notarized. Gatekeeper blocks it at the first start. Remove the quarantine flag once:

    xattr -dr com.apple.quarantine /Applications/ReStudio.app

Each installer has a .sha256 file next to it.
NOTES
)"

gh release create "$tag" "$@" \
  --repo "$repository" \
  --target "$commit" \
  --title "ReStudio ${tag#restudio/}" \
  --notes "$notes" \
  --prerelease

updates="$(mktemp -d)/updates.xml"
"$(dirname "$0")/make-updates-xml.sh" "$build" "${tag#restudio/v}" \
  "https://github.com/$repository/releases/tag/$tag" > "$updates"
cat "$updates"
if ! gh release view update-metadata --repo "$repository" > /dev/null 2>&1; then
  gh release create update-metadata --repo "$repository" --target "$commit" \
    --title "Update metadata" --notes "ReStudio reads updates.xml from this release. Do not delete it." --prerelease
fi
gh release upload update-metadata "$updates" --repo "$repository" --clobber
