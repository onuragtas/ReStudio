#!/usr/bin/env bash
# Writes the update metadata that ReStudio reads at -Didea.updates.url (see ReStudioBranding in build/src).
# The IDE shows an update if the build number in this file is higher than its own build number.
#
# Usage: fork/make-updates-xml.sh <build number> <version> <release URL> > updates.xml
set -euo pipefail

build="$1"
version="$2"
url="$3"
date="$(date -u +%Y%m%d)"

cat <<XML
<products>
  <product name="ReStudio">
    <code>IC</code>
    <channel id="RS-RELEASE" status="release" licensing="release">
      <build number="$build" version="$version" releaseDate="$date">
        <message>ReStudio $version is available.</message>
        <blogPost url="$url"/>
        <button name="Download" url="$url" download="true"/>
      </build>
    </channel>
  </product>
</products>
XML
