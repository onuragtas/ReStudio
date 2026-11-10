#!/usr/bin/env bash
# Signs, notarizes and staples a macOS .sit archive in place. The release workflow runs this script on its macOS
# runner, because the Linux build has no codesign. The local build signs with ReStudioMacSigning (build/src) instead.
#
# The script staples the app before it packs the .sit again. So the .sit and the .dmg both have the ticket, and
# Gatekeeper accepts the app with no network connection.
#
# Usage: fork/sign-mac.sh <sit> <entitlements>
# Environment:
#   RESTUDIO_MAC_SIGN_IDENTITY  "Developer ID Application: <team name> (<team ID>)". The keychain holds its key.
#   APPLE_ISSUER_ID, APPLE_KEY_ID, APPLE_PRIVATE_KEY_FILE  The App Store Connect API key for notarytool.
set -euo pipefail

sit="$(cd "$(dirname "$1")" && pwd)/$(basename "$1")"
entitlements="$(cd "$(dirname "$2")" && pwd)/$(basename "$2")"
: "${RESTUDIO_MAC_SIGN_IDENTITY:?}" "${APPLE_ISSUER_ID:?}" "${APPLE_KEY_ID:?}" "${APPLE_PRIVATE_KEY_FILE:?}"

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
ditto -x -k "$sit" "$work/app"
app="$(echo "$work"/app/*.app)"

python3 -I "$(dirname "$0")/sign-mac-app.py" "$app" "$RESTUDIO_MAC_SIGN_IDENTITY" "$entitlements"

ditto -c -k --norsrc --keepParent "$app" "$work/notarize.zip"
result="$(xcrun notarytool submit "$work/notarize.zip" --wait --timeout 3h --output-format json \
  --issuer "$APPLE_ISSUER_ID" --key-id "$APPLE_KEY_ID" --key "$APPLE_PRIVATE_KEY_FILE")"
echo "$result"
id="$(python3 -I -c 'import json, sys; print(json.loads(sys.argv[1])["id"])' "$result")"
status="$(python3 -I -c 'import json, sys; print(json.loads(sys.argv[1])["status"])' "$result")"
if [ "$status" != Accepted ]; then
  xcrun notarytool log "$id" --issuer "$APPLE_ISSUER_ID" --key-id "$APPLE_KEY_ID" --key "$APPLE_PRIVATE_KEY_FILE"
  echo "The notarization of $(basename "$sit") failed: $status" >&2
  exit 1
fi

xcrun stapler staple "$app"
spctl --assess --type execute --verbose "$app"
rm "$sit"
# No resource forks: --sequesterRsrc adds a __MACOSX directory, and staple.sh of the .dmg build expects one entry.
ditto -c -k --norsrc --keepParent "$app" "$sit"
echo "Signed, notarized and stapled $(basename "$sit")"
