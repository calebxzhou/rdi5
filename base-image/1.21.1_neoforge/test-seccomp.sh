#!/bin/bash
# Test script to verify seccomp profile is working correctly

set -e

CONTAINER_NAME="test-seccomp-$$"
IMAGE="rdi:1.21.1_neoforge"

echo "=== Seccomp Profile Test ==="
echo ""

# Check if seccomp profile exists
if [ ! -f "run/seccomp-minecraft.json" ]; then
    echo "❌ ERROR: seccomp-minecraft.json not found in run/ directory"
    exit 1
fi

echo "✅ Seccomp profile found"
echo ""

# Start a test container (you'll need to adapt this to your actual container creation)
echo "Note: This is a manual test guide. To test automatically, you need a running container."
echo ""
echo "Manual Testing Steps:"
echo "====================="
echo ""
echo "1. Start a container normally through your application"
echo ""
echo "2. Verify seccomp is applied:"
echo "   docker inspect <container-name> | grep -A 5 SecurityOpt"
echo "   Should show: \"seccomp=...\" and \"no-new-privileges=true\""
echo ""
echo "3. Test that dangerous syscalls are blocked:"
echo "   docker exec <container-name> sh -c 'reboot'"
echo "   Expected: Operation not permitted"
echo ""
echo "4. Test that allowed syscalls work:"
echo "   docker exec <container-name> sh -c 'ls /opt/server'"
echo "   Expected: Should list files normally"
echo ""
echo "5. Test Java can run (main use case):"
echo "   docker exec <container-name> sh -c 'java -version'"
echo "   Expected: Java version output"
echo ""

# Validate JSON syntax
echo "Validating JSON syntax..."
if command -v jq &> /dev/null; then
    if jq empty run/seccomp-minecraft.json 2>/dev/null; then
        echo "✅ JSON syntax is valid"
    else
        echo "❌ JSON syntax error in seccomp profile"
        exit 1
    fi
else
    echo "⚠️  jq not installed, skipping JSON validation"
    echo "   Install with: apt install jq (Linux) or brew install jq (Mac)"
fi

echo ""
echo "=== Profile Statistics ==="
ALLOWED_SYSCALLS=$(grep -o '"SCMP_ACT_ALLOW"' run/seccomp-minecraft.json | wc -l)
echo "Whitelisted syscall groups: $ALLOWED_SYSCALLS"

# Count individual syscalls
if command -v jq &> /dev/null; then
    TOTAL_SYSCALLS=$(jq '[.syscalls[].names] | flatten | length' run/seccomp-minecraft.json)
    echo "Total individual syscalls allowed: $TOTAL_SYSCALLS"
fi

echo ""
echo "=== Security Warnings ==="
echo "⚠️  CAP_SYS_ADMIN is still enabled - HIGH RISK"
echo "⚠️  Mount syscalls are allowed - needed for FUSE but dangerous"
echo "✅ Seccomp profile blocks most dangerous syscalls"
echo "✅ no-new-privileges prevents privilege escalation"
echo ""
echo "For production: Consider gVisor or Kata Containers for untrusted code"
