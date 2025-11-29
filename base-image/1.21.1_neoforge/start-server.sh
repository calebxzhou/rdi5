#!/bin/bash
set -euo pipefail

UNIVERSE_DIR="/data"
#uid=1000(rdi) gid=101(rdi) groups=101(rdi),101(rdi)
id rdi
echo "mounting data"

# 1. Mount tmpfs ONCE to a parent directory
# Combine the size (64M + 64M = 128M)
mount -t tmpfs -o size=128M tmpfs /mnt/overlay

# 2. Create the directories inside the SAME tmpfs instance
mkdir -p /mnt/overlay/upper
mkdir -p /mnt/overlay/work

# 3. CRITICAL: Ensure the target user (rdi/1000) owns the writable layers.
# Even with UID mapping, creating these with the correct owner prevents
# permission edge-cases on the underlying storage.
chown 1000:1000 /mnt/overlay/upper /mnt/overlay/work

echo "mounting ok"
ls -la /mnt >&2

# 4. Mount fuse-overlayfs
fuse-overlayfs \
    -o lowerdir=/mnt/modpack:/mnt/lib,\
upperdir=/mnt/overlay/upper,workdir=/mnt/overlay/work,\
allow_other \
    /opt/server

echo "fuse ok"
ls -la /opt/server >&2


# Drop to your normal user
cd /opt/server  
exec su-exec rdi java -Xmx4G @libraries/net/neoforged/neoforge/21.1.211/unix_args.txt --universe ${UNIVERSE_DIR} --nogui