package com.astraveil.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Spoof 档案 DAO（POJO 接口；可后续升级为 Room @Dao）。
 *
 * 推理：使用 Flow 暴露档案列表，UI 自动响应数据库变更。
 * 当前实现从内存 [com.astraveil.app.spoof.SPOOF_PROFILES] 读取；
 * 启用 Room 后，方法签名保持不变，只增加 @Query / @Insert 注解。
 */
interface SpoofProfileDao {

    fun observeAll(): Flow<List<SpoofProfileEntity>>

    fun observeBrands(): Flow<List<String>>

    suspend fun getByName(name: String): SpoofProfileEntity?

    suspend fun insertAll(profiles: List<SpoofProfileEntity>)

    suspend fun upsert(profile: SpoofProfileEntity)

    suspend fun deleteByName(name: String)

    suspend fun deleteAllCustom()

    suspend fun count(): Int
}

/**
 * 内存 DAO 实现 — 直接从 [com.astraveil.app.spoof.SPOOF_PROFILES] 读取。
 *
 * 推理：当 Room 不可用时（首次启动 / 数据库迁移失败 / build 未启用 Room），
 * 通过此实现提供只读访问，UI 仍能展示 48 台预设档案。
 */
class InMemorySpoofProfileDao : SpoofProfileDao {

    private val seed: List<SpoofProfileEntity> =
        com.astraveil.app.spoof.SPOOF_PROFILES.map {
            com.astraveil.app.spoof.SpoofProfileMapper.toEntity(it)
        }

    private val custom = mutableListOf<SpoofProfileEntity>()

    override fun observeAll(): Flow<List<SpoofProfileEntity>> =
        flowOf((seed + custom).sortedBy { it.name })

    override fun observeBrands(): Flow<List<String>> =
        flowOf((seed + custom).map { it.brand }.distinct().sorted())

    override suspend fun getByName(name: String): SpoofProfileEntity? =
        (seed + custom).firstOrNull { it.name == name }

    override suspend fun insertAll(profiles: List<SpoofProfileEntity>) {
        // No-op for seed; only custom profiles are mutable
        profiles.forEach { p ->
            custom.removeAll { it.name == p.name }
            custom.add(p)
        }
    }

    override suspend fun upsert(profile: SpoofProfileEntity) {
        custom.removeAll { it.name == profile.name }
        custom.add(profile)
    }

    override suspend fun deleteByName(name: String) {
        custom.removeAll { it.name == name }
    }

    override suspend fun deleteAllCustom() {
        custom.clear()
    }

    override suspend fun count(): Int = (seed + custom).size
}
