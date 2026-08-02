#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# AstraHub 验证 — Shell 包装器
#
# 用法:
#   ./tools/validate-hub.sh              # 验证默认索引
#   ./tools/validate-hub.sh --all        # 验证所有
#   ./tools/validate-hub.sh --strict     # 严格模式
# ─────────────────────────────────────────────────────────────────────────────

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

# 检查 Python
if ! command -v python3 &>/dev/null; then
    echo "错误: python3 未安装" >&2
    exit 1
fi

# 检查依赖（非致命）
python3 -c "import jsonschema" 2>/dev/null || {
    echo "警告: jsonschema 未安装，Schema 验证将跳过" >&2
    echo "安装: pip install jsonschema" >&2
}

python3 -c "import semver" 2>/dev/null || {
    echo "警告: semver 未安装，版本验证将跳过" >&2
    echo "安装: pip install semver" >&2
}

# 执行验证
exec python3 "$SCRIPT_DIR/validate-hub.py" "$@"
