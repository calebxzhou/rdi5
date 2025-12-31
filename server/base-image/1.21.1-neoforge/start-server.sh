#!/bin/bash
set -euo pipefail

UNIVERSE_DIR="/data"

ls -la /opt/server >&2
chown -R rdi:rdi /home/rdi /opt/server /data
# Drop to the non-root server user before launching Java
cd /opt/server
exec su-exec rdi java -Xmx4G @libraries/net/neoforged/neoforge/21.1.217/unix_args.txt --universe "${UNIVERSE_DIR}" --nogui