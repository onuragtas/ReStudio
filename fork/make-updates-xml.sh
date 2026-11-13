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
        <button name="Download" url="$url" download="true"/>$patch
      </build>
${older:+$older
}    </channel>
  </product>
</products>
XML
