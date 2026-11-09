#!/usr/bin/env bash
# Makes the update patches from the published release to the new build. The IDE downloads a patch for
# "Update and Restart" and applies it with the updater. Without a patch, the IDE offers only the download page.
#
# For each new Linux .tar.gz and macOS .sit in <dist>, the script downloads the same file of the published release
# and runs `updater create` on the two unpacked distributions. It handles one pair at a time, because each pair needs
# about 7 GB of disk.
#
# The patch names follow BaseJetBrainsExternalProductResourceUrls.computePatchUrl:
#   IC-<old build>-<new build>-patch[-aarch64]-<unix|mac>.jar
#
# Output: the patches in <out>, and <out>/patches.txt with the old build on the first line and one "<os> <size in MB>"
# line for each patch. fork/make-updates-xml.sh reads this file.
#
# Usage: fork/make-patches.sh <repository> <new build> <dist> <out>
# Needs: gh, Java 11 to 21 (JAVA_HOME or PATH). The updater uses Gradle 8.5, which does not run on Java 25.
set -euo pipefail

repository="$1"
build="$2"
dist="$3"
out="$4"
root="$(cd "$(dirname "$0")/.." && pwd)"
mkdir -p "$out"

metadata="$(gh release download update-metadata --repo "$repository" --pattern updates.xml --output - 2>/dev/null || true)"
old_build="$(sed -n 's/.*<build number="\([0-9.]*\)".*/\1/p' <<< "$metadata" | head -n 1)"
old_tag="$(sed -n 's#.*<blogPost url="[^"]*/releases/tag/\([^"]*\)".*#\1#p' <<< "$metadata" | head -n 1)"
if [ -z "$old_build" ] || [ -z "$old_tag" ]; then
  echo "No published release. The script makes no patches."
  exit 0
fi
if [ "$old_build" = "$build" ]; then
  echo "The published build is $build already. The script makes no patches." >&2
  exit 1
fi
echo "Patches from $old_build ($old_tag) to $build"

java="${JAVA_HOME:+$JAVA_HOME/bin/}java"
(cd "$root/updater" && ./gradlew --quiet fatJar)
updater="$root/updater/build/libs/updater-full.jar"

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
echo "$old_build" > "$out/patches.txt"

unpack() {
  local archive="$1" dir="$2"
  mkdir -p "$dir"
  case "$archive" in
    *.tar.gz) tar -xzf "$archive" -C "$dir" --strip-components 1 ;;
    *.sit) unzip -q "$archive" -d "$dir" ;;
  esac
}

for arch in "" -aarch64; do
  for ext in tar.gz sit; do
    new="$dist/ReStudio-$build$arch.$ext"
    old_name="ReStudio-$old_build$arch.$ext"
    [ -f "$new" ] || continue
    if ! gh release download "$old_tag" --repo "$repository" --pattern "$old_name" --dir "$work" 2>/dev/null; then
      echo "The release $old_tag has no $old_name. The script makes no patch for it."
      continue
    fi
    if [ "$ext" = tar.gz ]; then os=unix; options=(); else os=mac; options=(--root=Contents); fi
    unpack "$work/$old_name" "$work/old"
    unpack "$new" "$work/new"
    if [ "$os" = mac ]; then
      old_dir="$(echo "$work"/old/*.app)"
      new_dir="$(echo "$work"/new/*.app)"
    else
      old_dir="$work/old"
      new_dir="$work/new"
    fi
    patch="$out/IC-$old_build-$build-patch$arch-$os.jar"
    "$java" -Xmx2g -cp "$updater" com.intellij.updater.Runner create "$old_build" "$build" "$old_dir" "$new_dir" "$patch" \
      --strict --jar="$updater" "${options[@]}"
    size=$(( ($(wc -c < "$patch") + 1048575) / 1048576 ))
    echo "$os $size" >> "$out/patches.txt"
    echo "Made $(basename "$patch") ($size MB)"
    rm -rf "${work:?}/old" "${work:?}/new" "${work:?}/$old_name"
  done
done
