#!/bin/bash
set -euo pipefail

START_PARAMS="${START_PARAMS:-"-Xmx8G @libraries/net/neoforged/neoforge/21.1.217/unix_args.txt --universe /data --nogui"}"

ls -la /opt/server >&2
chown -R rdi:rdi /home/rdi /opt/server /data
# Drop to the non-root server user before launching Java
cd /opt/server
eval "JAVA_ARGS=(${START_PARAMS})"
exec su-exec rdi java "${JAVA_ARGS[@]}"