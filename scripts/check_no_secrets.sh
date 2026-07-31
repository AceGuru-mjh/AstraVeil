#!/usr/bin/env bash
set -euo pipefail
echo "=== Scanning for committed secrets ==="
FAIL=0
if git ls-files | grep -qE "\\.jks$|\\.keystore$"; then
    echo "FATAL: keystore tracked in git"; git ls-files | grep -E "\\.jks$|\\.keystore$"; FAIL=1
fi
if grep -rnE "(storePassword|keyPassword|password)\s*=\s*[\"'][^\"']+[\"']" --include="*.kts" --include="*.gradle" --exclude="*example*" --exclude="*test*" . 2>/dev/null | grep -vE "System\.getenv|providers\.gradleProperty|findProperty|secrets\."; then
    echo "FATAL: hardcoded password"; FAIL=1
fi
if [[ $FAIL -ne 0 ]]; then echo "=== FAILED ==="; exit 1; fi
echo "=== PASSED ==="
