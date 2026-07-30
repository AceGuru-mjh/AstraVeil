#!/system/bin/sh
# AstraVeil daemon launcher.
# Executed by Magisk at boot (late_start service mode).

MODDIR=${0%/*}
DAEMON="$MODDIR/bin/astrad"
SOCK_DIR="/dev/astra"
LOG_FILE="/data/astra/logs/astrad.log"

# Wait for /data to be fully mounted
while [ ! -d "/data/adb" ]; do
    sleep 0.5
done

# Create runtime directories
mkdir -p "$SOCK_DIR"
mkdir -p /data/astra/logs
mkdir -p /data/astra/modules
chmod 0755 "$SOCK_DIR"

# Check daemon binary exists
if [ ! -f "$DAEMON" ]; then
    echo "astrad binary not found at $DAEMON" >> "$LOG_FILE"
    exit 1
fi

chmod 0755 "$DAEMON"

# Kill any previous instance
if pidof astrad > /dev/null 2>&1; then
    killall astrad 2>/dev/null
    sleep 1
fi

# Start daemon in background
echo "Starting astrad at $(date)" >> "$LOG_FILE"
"$DAEMON" >> "$LOG_FILE" 2>&1 &

# Verify it started
sleep 1
if pidof astrad > /dev/null 2>&1; then
    echo "astrad started (pid=$(pidof astrad))" >> "$LOG_FILE"
else
    echo "astrad FAILED to start" >> "$LOG_FILE"
fi
