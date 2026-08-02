#!/system/bin/sh
# 在 /data 挂载后、Zygote 启动前执行

# 确保配置目录存在（首次安装或 data 擦除后）
mkdir -p /data/adb/astraveil/spoof
chmod 0755 /data/adb/astraveil
chmod 0755 /data/adb/astraveil/spoof
