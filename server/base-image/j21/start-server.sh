#!/bin/bash
set -euo pipefail

START_PARAMS="${START_PARAMS:-"-Xmx8G @libraries/net/neoforged/neoforge/21.1.217/unix_args.txt --universe /data --nogui"}"

#ls -la /opt/server >&2
chown -R rdi:rdi /home/rdi /opt/server /data

# Get the Docker host IP (default gateway)
HOST_IP=$(ip route | grep default | awk '{print $3}')
echo "Detected Docker host IP: ${HOST_IP}" >&2

# Whitelist configuration (comma-separated ip:port pairs)
# Default: host machine IP with port 65231
# Format: "ip1:port1,ip2:port2" e.g., "192.168.1.1:65231,10.0.0.1:443"
OUTGOING_WHITELIST="${OUTGOING_WHITELIST:-${HOST_IP}:65231}"

# Restrict outgoing connections to whitelisted ip:port combinations only
echo "Applying firewall rules..." >&2

# Flush existing rules to be safe
iptables -F INPUT
iptables -F OUTPUT

# Allow all incoming connections
iptables -P INPUT ACCEPT

# Allow loopback (localhost)
iptables -A OUTPUT -o lo -j ACCEPT

# Allow established and related connections (allows replies to incoming connections)
iptables -A OUTPUT -m conntrack --ctstate RELATED,ESTABLISHED -j ACCEPT

# Allow outgoing connections to whitelisted ip:port combinations
IFS=',' read -ra WHITELIST_ENTRIES <<< "${OUTGOING_WHITELIST}"
for entry in "${WHITELIST_ENTRIES[@]}"; do
    entry=$(echo "${entry}" | xargs)  # trim whitespace
    if [ -n "${entry}" ]; then
        # Parse ip:port format
        host="${entry%:*}"
        port="${entry##*:}"
        
        if [ -n "${host}" ] && [ -n "${port}" ]; then
            echo "Allowing outgoing to ${host}:${port}" >&2
            iptables -A OUTPUT -d "${host}" -p tcp --dport "${port}" -j ACCEPT
            iptables -A OUTPUT -d "${host}" -p udp --dport "${port}" -j ACCEPT
        fi
    fi
done

# Set default policy to DROP for all other outgoing connections
iptables -P OUTPUT DROP

# Drop to the non-root server user before launching Java
cd /opt/server
eval "JAVA_ARGS=(${START_PARAMS})"
exec gosu rdi java "${JAVA_ARGS[@]}"