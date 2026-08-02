package com.astraveil.app.spoof

import com.astraveil.app.data.SpoofProfileEntity

/**
 * Domain ↔ Entity 双向映射。
 *
 * 推理：
 *   - [SpoofProfile] 是 UI/业务层使用的纯 Kotlin data class
 *   - [SpoofProfileEntity] 是 Room 持久化层使用的 @Entity
 *   - 分离避免 Room 注解污染业务层，也便于切换持久化后端
 */
object SpoofProfileMapper {

    /** Entity → Domain */
    fun toDomain(e: SpoofProfileEntity): SpoofProfile = SpoofProfile(
        name = e.name,
        brand = e.brand,
        manufacturer = e.manufacturer,
        model = e.model,
        device = e.device,
        productName = e.productName,
        platform = e.platform,
        soc = e.soc,
        socManufacturer = e.socManufacturer,
        firstApi = e.firstApi,
        release = e.release,
        buildId = e.buildId,
        incremental = e.incremental,
        securityPatch = e.securityPatch,
        fingerprint = e.fingerprint,
        displayId = e.displayId,
        characteristics = e.characteristics,
        confidence = runCatching { DataConfidence.valueOf(e.confidence) }
            .getOrDefault(DataConfidence.HIGH),
    )

    /** Domain → Entity */
    fun toEntity(p: SpoofProfile, isCustom: Boolean = false): SpoofProfileEntity =
        SpoofProfileEntity(
            name = p.name,
            brand = p.brand,
            manufacturer = p.manufacturer,
            model = p.model,
            device = p.device,
            productName = p.productName,
            platform = p.platform,
            soc = p.soc,
            socManufacturer = p.socManufacturer,
            firstApi = p.firstApi,
            release = p.release,
            buildId = p.buildId,
            incremental = p.incremental,
            securityPatch = p.securityPatch,
            fingerprint = p.fingerprint,
            displayId = p.displayId,
            characteristics = p.characteristics,
            confidence = p.confidence.name,
            isCustom = isCustom,
        )
}
