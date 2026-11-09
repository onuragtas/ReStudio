---
name: restudio-release
description: Build the ReStudio Linux and macOS installers on the local Mac and publish them as a GitHub release.
---

# ReStudio local release

The Mac builds the Linux and macOS installers for x64 and aarch64 in one build. Then the Mac publishes them as a GitHub release, and it publishes `updates.xml` for the in-IDE update check. The CI workflow `.github/workflows/fork-release.yml` does the same work on GitHub runners. The two use the same scripts in `fork/`.

A release is public. Get the confirmation of the user for the tag before step 6. A confirmation for an earlier release does not apply to a new one.

## 1. Check the state

Stop and tell the user if one check fails.

```bash
git switch main && git fetch origin
git status --short                       # must be empty
git rev-list --count origin/main..HEAD   # must be 0: the release commit must be on GitHub
tag=$(tr -d '[:space:]' < fork/UPSTREAM_BASE)
[ "$(git -C android rev-parse HEAD)" = "$(git -C android rev-parse "refs/tags/$tag^{commit}")" ] && echo "android is at $tag"
gh auth status
gh release list --repo onuragtas/ReStudio --limit 5
```

If `android/` is not at the base tag, move it: `git -C android fetch origin "refs/tags/$tag" && git -C android switch --detach FETCH_HEAD`.

Ask the user for the version. The tag is `restudio/v<version>`, for example `restudio/v0.1.0`. The tag must not exist.

## 2. Build

```bash
fork/fetch-bundled-plugins.sh
build=$(fork/next-build-number.sh)
set -a; source ~/.restudio-dev/signing.env; set +a
export APPLE_PRIVATE_KEY="$(cat "$RESTUDIO_NOTARY_KEY_FILE")"
./installers.cmd -Dbuild.number="$build" -Dintellij.build.skip.build.steps=windows_artifacts
```

- `next-build-number.sh` reads the published `updates.xml` and prints a higher number. The IDE shows an update only for a higher build number.
- ReStudio has no Windows distribution, so the build skips the Windows steps.
- `~/.restudio-dev/signing.env` holds the signing certificate name and the notarization key. It is not in a repository. With it, `ReStudioMacSigning` (`build/src`) signs the macOS distribution with `codesign`, and the build notarizes and staples each `.sit` and `.dmg`. Apple can take from minutes to hours for a notarization. Without the file, the build makes unsigned installers. Then publish without `SIGNED=true` in step 6.
- Run the build in the background. With a warm Bazel cache, the build took 5 minutes on an M1 Pro (10 November 2026). After an upstream merge, the build compiles all modules again and takes about 40 minutes.

The build writes the installers to `out/idea-ce/artifacts`. A name without `-aarch64` is the x64 installer.

- `ReStudio-$build.tar.gz` and `ReStudio-$build-aarch64.tar.gz`: Linux
- `ReStudio-$build.dmg` and `ReStudio-$build-aarch64.dmg`: macOS
- `ReStudio-$build.sit` and `ReStudio-$build-aarch64.sit`: macOS, as a zip archive

## 3. Check the installers

```bash
artifacts=out/idea-ce/artifacts
fork/check-bundled-plugins.sh "$artifacts/ReStudio-$build.tar.gz" "$artifacts/ReStudio-$build-aarch64.tar.gz"
app=$(mktemp -d)
ditto -x -k "$artifacts/ReStudio-$build-aarch64.sit" "$app"
fork/smoke-test.sh "$app"/*.app/Contents/MacOS/restudio
spctl --assess --type execute --verbose "$app"/*.app   # signed build only: must say "accepted" and "Notarized Developer ID"
```

The smoke test must print "The IDE loaded the bundled plugins without errors". It checks only the macOS aarch64 installer. The Mac has no Docker, so it cannot run the Linux smoke test. Tell the user which installers had no smoke test.

## 4. Make the checksums

```bash
dist=$(mktemp -d)
for arch in "" -aarch64; do for ext in tar.gz dmg sit; do
  file="ReStudio-$build$arch.$ext"
  cp "$artifacts/$file" "$dist/"
  (cd "$dist" && shasum --algorithm 256 --binary "$file" > "$file.sha256")
done; done
ls -l "$dist"
```

## 5. Get the confirmation

Show the user the tag, the build number, the commit (`git rev-parse --short HEAD`) and the file list. Publish only after the user agrees.

## 6. Publish

```bash
SIGNED=true fork/publish-release.sh onuragtas/ReStudio "restudio/v<version>" "$(git rev-parse HEAD)" "$build" "$dist"/*
```

The script makes the update patches from the published release (`fork/make-patches.sh`), creates the tag and the release, then uploads the patches and `updates.xml` to the `update-metadata` release. The patch step needs Java 11 or later, for example `JAVA_HOME=/opt/homebrew/opt/openjdk@25`, and about 7 GB of free disk.

## 7. Check the result

- `gh release view "restudio/v<version>" --repo onuragtas/ReStudio` shows twelve files: six installers and their `.sha256` files.
- `gh release download update-metadata --repo onuragtas/ReStudio --pattern updates.xml --output -` shows the new build number.
- The tag push starts `fork-release.yml`. Its `guard` job finds the release and skips the build. Check it with `gh run list --repo onuragtas/ReStudio --workflow fork-release.yml --limit 1`. Cancel the run if it builds.

Report the release URL to the user. Also say what you did not check, for example the Linux smoke test.
