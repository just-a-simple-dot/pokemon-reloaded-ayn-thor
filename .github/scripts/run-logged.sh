#!/usr/bin/env bash
# Runs a command and, if it fails, repeats the end of its output as an error annotation. Job logs
# need a signed-in account to download, but annotations are public, so a failure can be diagnosed
# from the run page (or the API) without one.
set -uo pipefail
log="$(mktemp)"
"$@" 2>&1 | tee "$log"
status=${PIPESTATUS[0]}
if [ "$status" -ne 0 ]; then
  tail -n 40 "$log" | sed -e 's/%/%25/g' -e 's/\r//g' | awk 'BEGIN { ORS = "%0A" } { print }' > "$log.tail"
  echo "::error title=$1 falló (código $status)::$(cat "$log.tail")"
fi
exit "$status"
