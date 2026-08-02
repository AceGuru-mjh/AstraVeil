# AstraVeil Proto — Consumer ProGuard Rules
# 这些规则会自动应用到所有依赖 :proto 的模块

# 保留所有 protobuf 生成类
-keep class com.astraveil.proto.** { *; }

# 保留 protobuf-lite 运行时
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**

# 保留反射访问（protobuf 内部使用）
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

---

## 文件 9：`proto/proguard-rules.pro`（新增）
