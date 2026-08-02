#!/system/bin/sh
# 在系统启动完成后执行（late_start 模式）

MODDIR=${0%/*}
CONFIG_DIR="/data/adb/astraveil/spoof"

# ── 全局 resetprop 兜底（方案三） ──
# 推理：即使 Zygisk/LSPosed 未生效（如被禁用），
# 全局 resetprop 仍能修改所有新进程的属性值。
# 这是最弱但最可靠的层。

if [ -f "$CONFIG_DIR/global.json" ]; then
    # 检查 global.json 中是否有 resetprop_on_boot 标记
    if grep -q '"resetprop_on_boot":true' "$CONFIG_DIR/global.json" 2>/dev/null; then
        # 提取关键属性并 resetprop
        # 推理：不解析完整 JSON（shell 中无 jq），只提取已知 key
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

# ── 确保配置文件权限正确 ──
chmod 0644 $CONFIG_DIR/*.json 2>/dev/null
