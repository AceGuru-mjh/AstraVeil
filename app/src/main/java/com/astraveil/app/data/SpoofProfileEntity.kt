package com.astraveil.app.data

/**
 * 持久化伪装档案（POJO；可后续升级为 Room @Entity）。
 *
 * 推理：48 台预设档案 + 用户自定义档案都持久化到 SQLite，
 * 这样首次启动后无需重新解析 [com.astraveil.app.spoof.SPOOF_PROFILES]。
 * 主键 [name] 与 [com.astraveil.app.spoof.SpoofProfile.name] 一致，
 * 便于 upsert 与按名查询。
 *
 * 注：当前实现为纯 Kotlin POJO。当 build.gradle 启用 Room（kapt/ksp
 * + androidx.room:runtime）后，可加上 @Entity / @PrimaryKey 注解即可
 * 升级为 Room 实体，无需修改调用方代码。
 */
data class SpoofProfileEntity(
    val name: String,
    val brand: String,
    val manufacturer: String,
    val model: String,
    val device: String,
    val productName: String,
    val platform: String,
    val soc: String,
    val socManufacturer: String,
    val firstApi: Int,
    val release: String,
    val buildId: String,
    val incremental: String,
    val securityPatch: String,
    val fingerprint: String,
    val displayId: String,
    val characteristics: String,
    val confidence: String,   // "HIGH" | "MEDIUM"
    val isCustom: Boolean = false,
)
