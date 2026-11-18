#!/bin/sh
# Runs koppter from this checkout until it is quit.
# The exit code 55 means that "Restart to update" was clicked: pull, build and start again.
# Any other exit code ends the loop.
set -e
cd "$(dirname "$0")/.."
while :; do
    # Without a network, the application starts as it is.
    git pull --no-rebase || echo "run-loop: not pulled, starting what is here" >&2
    # Tells the application which commit it runs, and that its exit with 55 starts it again.
    KOPPTER_RUN_LOOP=$(git rev-parse HEAD)
    export KOPPTER_RUN_LOOP
    ./gradlew :app:createDistributable
    # The window may open on another desktop; the line tells that the application runs.
    echo "Running koppter..."
    set +e
    if [ "$(uname -s)" = Darwin ]; then
        app/build/compose/binaries/main/app/koppter.app/Contents/MacOS/koppter
    else
        app/build/compose/binaries/main/app/koppter/bin/koppter
    fi
    code=$?
    set -e
    if [ "$code" -ne 55 ]; then
        echo "koppter finished"
        exit "$code"
    fi
done
