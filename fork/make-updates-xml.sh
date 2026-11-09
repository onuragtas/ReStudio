#!/usr/bin/env bash
# Writes the update metadata that ReStudio reads at -Didea.updates.url (see ReStudioBranding in build/src).
# The IDE shows an update if the build number in this file is higher than its own build number.
#
# With a patches.txt file of fork/make-patches.sh, the file also lists the patch from the old build. The IDE then
# offers "Update and Restart". The "exclusions" attribute names each OS without a patch.
#
# Usage: fork/make-updates-xml.sh <build number> <version> <release URL> [<patches.txt>] > updates.xml
set -euo pipefail

build="$1"
version="$2"
url="$3"
patches="${4:-}"
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

cat <<XML
<products>
  <product name="ReStudio">
    <code>IC</code>
    <channel id="RS-RELEASE" status="release" licensing="release">
      <build number="$build" version="$version" releaseDate="$date">
        <message>ReStudio $version is available.</message>
        <blogPost url="$url"/>
        <button name="Download" url="$url" download="true"/>$patch
      </build>
    </channel>
  </product>
</products>
XML
