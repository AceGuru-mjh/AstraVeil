#!/system/bin/sh
MODDIR=${0%/*}
CONFIG_DIR="/data/adb/astraveil/spoof"

if [ -f "$CONFIG_DIR/global.json" ]; then
    if grep -q '"resetprop_on_boot":true' "$CONFIG_DIR/global.json" 2>/dev/null; then
        for key in ro.product.model ro.product.brand ro.product.manufacturer \
                   ro.product.device ro.product.name ro.build.fingerprint \
                   ro.build.display.id ro.build.id; do
            value=$(grep -o "\"$key\":\"[^\"]*\"" "$CONFIG_DIR/global.json" \
                    | head -1 | cut -d'"' -f4)
            if [ -n "$value" ]; then
                resetprop "$key" "$value"
            fi
        done
    fi
fi

chmod 0644 $CONFIG_DIR/*.json 2>/dev/null
