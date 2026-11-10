#!/usr/bin/env bash
# Starts an installed IDE with a small Python and Dart project, waits for indexing, and closes the IDE.
# It stops with an error if Dart, Flutter, LSP4IJ, Python or a ReStudio language plugin does not load, or if one of them
# logs an error.
# The release workflow runs it on Linux.
#
# Usage: fork/smoke-test.sh <launcher command>
#   Linux:   fork/smoke-test.sh xvfb-run -a <ide>/bin/restudio.sh
#   macOS:   fork/smoke-test.sh <app>/Contents/MacOS/restudio
set -euo pipefail

timeout_seconds="${SMOKE_TEST_TIMEOUT:-900}"
work="$(mktemp -d)"
if command -v cygpath > /dev/null; then work="$(cygpath -m "$work")"; fi

mkdir -p "$work/project"
printf 'print("ReStudio")\n' > "$work/project/main.py"
printf 'void main() {\n  print("ReStudio");\n}\n' > "$work/project/main.dart"
printf '%%waitForSmart\n%%exitApp\n' > "$work/script.txt"

cat > "$work/idea.properties" <<EOF
idea.config.path=$work/config
idea.system.path=$work/system
idea.log.path=$work/log
idea.plugins.path=$work/plugins
idea.trust.all.projects=true
jb.consents.confirmation.enabled=false
jb.privacy.policy.text=<!--999.999-->
idea.initially.ask.config=never
intellij.startup.wizard=false
testscript.filename=$work/script.txt
EOF
export RESTUDIO_PROPERTIES="$work/idea.properties"

echo "Starting the IDE in $work"
"$@" "$work/project" > "$work/stdout.log" 2>&1 &
pid=$!

status=0
for ((i = 0; i < timeout_seconds; i += 5)); do
  if ! kill -0 "$pid" 2> /dev/null; then break; fi
  sleep 5
done
if kill -0 "$pid" 2> /dev/null; then
  echo "The IDE did not close in $timeout_seconds seconds" >&2
  kill "$pid" 2> /dev/null || true
  status=1
fi
wait "$pid" 2> /dev/null || true

log="$work/log/idea.log"
if [[ ! -f "$log" ]]; then
  echo "The IDE wrote no log. Output:" >&2
  cat "$work/stdout.log" >&2
  exit 1
fi

loaded="$(awk '/Loaded (bundled|custom) plugins:/' "$log")"
for id in "(Dart," "(io.flutter," "(com.redhat.devtools.lsp4ij," "(PythonCore," "(dev.restudio.core," "(dev.restudio.go," \
  "(dev.restudio.dotnet," "(dev.restudio.web," "(dev.restudio.php,"; do
  if [[ "$loaded" != *"$id"* ]]; then
    echo "The IDE did not load the plugin $id" >&2
    status=1
  fi
done

errors="$(awk '/ SEVERE - /' "$log")"
if [[ -n "$errors" ]]; then
  echo "The log has errors:"
  echo "$errors"
  if awk '/Plugin to blame: (Dart|Flutter|LSP4IJ|Python|ReStudio)|ClassNotFoundException|NoClassDefFoundError|PluginException/' <<< "$errors" | awk 'END { exit NR == 0 }'; then
    echo "A bundled plugin of ReStudio caused an error" >&2
    status=1
  fi
fi

if [[ $status -ne 0 ]]; then
  echo "--- idea.log (last 200 lines) ---"
  tail -n 200 "$log"
fi
[[ $status -eq 0 ]] && echo "The IDE loaded the bundled plugins without errors"
exit $status
