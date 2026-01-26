#!/bin/bash
set -euo pipefail

START_PARAMS="${START_PARAMS:-"-Xmx8G @libraries/net/neoforged/neoforge/21.1.217/unix_args.txt --universe /data --nogui"}"

ls -la /opt/server >&2
chown -R rdi:rdi /home/rdi /opt/server /data

# Restrict outgoing connections to LAN and Localhost only, but allow replies to incoming
# echo "Applying firewall rules..."
# # Flush existing rules to be safe
# iptables -F INPUT
# iptables -F OUTPUT

# # Allow all incoming connections (default is usually ACCEPT, but being explicit)
# iptables -P INPUT ACCEPT

# # Allow loopback
# iptables -A OUTPUT -o lo -j ACCEPT

# # Allow established and related connections (allows replies to incoming connections from anywhere)
# iptables -A OUTPUT -m conntrack --ctstate RELATED,ESTABLISHED -j ACCEPT

# # Allow outgoing connections to standard private networks (RFC 1918)
# iptables -A OUTPUT -d 10.0.0.0/8 -j ACCEPT
# iptables -A OUTPUT -d 172.16.0.0/12 -j ACCEPT
# iptables -A OUTPUT -d 192.168.0.0/16 -j ACCEPT

# # Set default policy to DROP for other outgoing connections
# iptables -P OUTPUT DROP

# Drop to the non-root server user before launching Java
cd /opt/server
eval "JAVA_ARGS=(${START_PARAMS})"
exec gosu rdi java "${JAVA_ARGS[@]}"