#!/usr/bin/env bash
# Publishes the installers as a GitHub release, then publishes updates.xml on the update-metadata release.
# The IDE reads updates.xml from there (ReStudioBranding.UPDATES_URL in build/src).
# Before that, fork/make-patches.sh makes the update patches from the published release. The patches go to the
# update-metadata release too (ReStudioExternalResourceUrls.basePatchDownloadUrl).
# The release workflow and the local release (.claude/skills/restudio-release) use this script.
#
# Usage: [SIGNED=true] [PATCHES_DIR=<dir>] fork/publish-release.sh <repository> <tag> <commit> <build number> <file>...
#   <tag> is restudio/v<version>, for example restudio/v0.1.0.
set -euo pipefail

repository="$1"
tag="$2"
commit="$3"
build="$4"
shift 4

# PATCHES_DIR: the patches are ready, for example from fork/merge-patches.sh in the release workflow.
# Without it, the script makes the patches for all the files here.
if [ -n "${PATCHES_DIR:-}" ]; then
  patches="$PATCHES_DIR"
else
  patches="$(mktemp -d)"
  "$(dirname "$0")/make-patches.sh" "$repository" "$build" "$(dirname "$1")" "$patches"
fi

# SIGNED=true: the macOS installers are signed and notarized (ReStudioMacSigning in build/src).
if [ "${SIGNED:-false}" = true ]; then
  notes="$(cat <<'NOTES'
Preview build of ReStudio.

macOS: the app is signed and notarized.

Each installer has a .sha256 file next to it.
NOTES
)"
else
  notes="$(cat <<'NOTES'
Unsigned preview build of ReStudio.

macOS: the app is not signed or notarized. Gatekeeper blocks it at the first start. Remove the quarantine flag once:

    xattr -dr com.apple.quarantine /Applications/ReStudio.app

Each installer has a .sha256 file next to it.
NOTES
)"
fi

gh release create "$tag" "$@" \
  --repo "$repository" \
  --target "$commit" \
  --title "ReStudio ${tag#restudio/}" \
  --notes "$notes" \
  --prerelease

updates="$(mktemp -d)/updates.xml"
# The published updates.xml keeps the older builds, so the IDE can chain the patches.
published="$(dirname "$updates")/published.xml"
gh release download update-metadata --repo "$repository" --pattern updates.xml --output "$published" 2>/dev/null || : > "$published"
"$(dirname "$0")/make-updates-xml.sh" "$build" "${tag#restudio/v}" \
  "https://github.com/$repository/releases/tag/$tag" "$([ -f "$patches/patches.txt" ] && echo "$patches/patches.txt")" \
  "$published" > "$updates"
cat "$updates"
if ! gh release view update-metadata --repo "$repository" > /dev/null 2>&1; then
  gh release create update-metadata --repo "$repository" --target "$commit" \
    --title "Update metadata" --notes "ReStudio reads updates.xml from this release. Do not delete it." --prerelease
fi
for jar in "$patches"/*.jar; do
  [ -f "$jar" ] && gh release upload update-metadata "$jar" --repo "$repository" --clobber
done
gh release upload update-metadata "$updates" --repo "$repository" --clobber
