# Commands for developers, run with just (https://just.systems).
set windows-shell := ["cmd.exe", "/c"]

alias loop := run-loop

# Run the desktop application from this checkout until it is quit: "Restart to update" pulls, builds and starts it again.
[unix]
run-loop:
    scripts/run-loop.sh

[windows]
run-loop:
    scripts\run-loop.cmd
