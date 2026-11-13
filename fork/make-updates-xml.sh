#!/usr/bin/env bash
# Writes the update metadata that ReStudio reads at -Didea.updates.url (see ReStudioBranding in build/src).
# The IDE shows an update if the build number in this file is higher than its own build number.
#
# With a patches.txt file of fork/make-patches.sh, the file also lists the patch from the old build. The IDE then
# offers "Update and Restart". The "exclusions" attribute names each OS without a patch.
#
# With the published updates.xml, the file also keeps the older builds and their patches. The IDE then finds a chain
# of patches (UpdateStrategy in platform-impl). For example, a user of 1001 updates to 1018 through 1001 -> 1017 and
# 1017 -> 1018. Without a chain, the IDE offers only the download page.
#
# With ASSETS_DIR and ASSETS_URL, the file also lists the full installers with their SHA-256 (<restudio-package>).
# A build without a chain of patches then downloads the full installer and replaces itself (ReStudioFullUpdate in
# platform-impl/update-checker). ASSETS_DIR holds the installers and their .sha256 files. ASSETS_URL is the download URL
# of the release, with a trailing slash. The macOS installer is listed only with SIGNED=true, because the IDE accepts only
# an app with the Developer ID signature of the running app.
#
# Usage: fork/make-updates-xml.sh <build number> <version> <release URL> [<patches.txt>] [<published updates.xml>]
# KEEP_BUILDS: the number of older builds to keep, 20 by default.
set -euo pipefail

build="$1"
version="$2"
url="$3"
patches="${4:-}"
published="${5:-}"
date="$(date -u +%Y%m%d)"

patch=""
if [ -n "$patches" ] && [ "$(wc -l < "$patches")" -gt 1 ]; then
  from="$(head -n 1 "$patches")"
  size="$(tail -n +2 "$patches" | awk '$2 > max { max = $2 } END { print max }')"
  exclusions=""
  for os in mac unix win; do
    if ! tail -n +2 "$patches" | grep -q "^$os "; then
      exclusions="${exclusions:+$exclusions,}$os"
    fi
  done
  patch="
        <patch from=\"$from\" size=\"$size\"${exclusions:+ exclusions=\"$exclusions\"}/>"
fi

packages=""
if [ -n "${ASSETS_DIR:-}" ] && [ -n "${ASSETS_URL:-}" ]; then
  for entry in "mac aarch64 ReStudio-$build-aarch64.sit" "linux x64 ReStudio-$build.tar.gz" \
               "linux aarch64 ReStudio-$build-aarch64.tar.gz"; do
    read -r os arch file <<< "$entry"
    if [ "$os" = mac ] && [ "${SIGNED:-false}" != true ]; then
      continue
    fi
    [ -f "$ASSETS_DIR/$file.sha256" ] || continue
    sha256="$(awk '{ print tolower($1); exit }' "$ASSETS_DIR/$file.sha256")"
    if ! [[ "$sha256" =~ ^[0-9a-f]{64}$ ]]; then
      echo "Invalid SHA-256 in $ASSETS_DIR/$file.sha256" >&2
      exit 1
    fi
    size=""
    if [ -f "$ASSETS_DIR/$file" ]; then
      size=" size=\"$(wc -c < "$ASSETS_DIR/$file" | tr -d ' ')\""
    fi
    packages="$packages
        <restudio-package os=\"$os\" arch=\"$arch\" url=\"$ASSETS_URL$file\" sha256=\"$sha256\"$size/>"
  done
fi

# The <build> elements of the published file, without the new build. Each element starts on a "<build " line and
# ends on a "</build>" line, as this script writes them.
older=""
if [ -n "$published" ] && [ -s "$published" ]; then
  older="$(awk -v skip="$build" -v keep="${KEEP_BUILDS:-20}" '
    /<build / { inside = 1; block = ""; drop = index($0, "number=\"" skip "\"") > 0 }
    inside { block = block $0 "\n" }
    /<\/build>/ && inside { inside = 0; if (!drop && count < keep) { printf "%s", block; count++ } }
  ' "$published")"
fi

cat <<XML
<products>
  <product name="ReStudio">
    <code>IC</code>
    <channel id="RS-RELEASE" status="release" licensing="release">
      <build number="$build" version="$version" releaseDate="$date">
        <message>ReStudio $version is available.</message>
        <blogPost url="$url"/>
        <button name="Download" url="$url" download="true"/>$patch$packages
      </build>
${older:+$older
}    </channel>
  </product>
</products>
XML
