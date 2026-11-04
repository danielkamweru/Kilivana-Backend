#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

log_file="${STARTUP_LOG:-/tmp/kilivana-startup.log}"
umask 077

printf 'Starting Kilivana Backend. Startup output: %s\n' "$log_file"
set +e
./mvnw spring-boot:run 2>&1 | tee "$log_file"
status=${PIPESTATUS[0]}
set -e

if (( status != 0 )); then
    printf '\nSpring Boot exited with status %d. Review the first startup error in %s.\n' \
        "$status" "$log_file" >&2
fi

exit "$status"
