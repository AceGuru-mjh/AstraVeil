SKIPUNZIP=1

ui_print "- Extracting module files"
unzip -o "$ZIPFILE" -x 'META-INF/*' -d $MODPATH >&2

ui_print "- Setting permissions"
set_perm_recursive $MODPATH 0 0 0755 0644
set_perm_recursive $MODPATH/zygisk 0 0 0755 0755

ui_print "- Creating config directory"
mkdir -p /data/adb/astraveil/spoof
chmod 0755 /data/adb/astraveil
chmod 0755 /data/adb/astraveil/spoof
chmod 0644 /data/adb/astraveil/spoof/*.json 2>/dev/null

ui_print "- AstraVeil Spoof Engine installed"
