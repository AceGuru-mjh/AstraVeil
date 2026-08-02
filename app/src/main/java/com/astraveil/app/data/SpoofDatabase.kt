package com.astraveil.app.data

import android.content.Context

/**
 * Spoof 档案数据库入口（POJO；可后续升级为 Room @Database）。
 *
 * 推理：
 *   1. 当前实现使用 [InMemorySpoofProfileDao] 从内存
 *      [com.astraveil.app.spoof.SPOOF_PROFILES] 读取，无需持久化。
 *   2. 启用 Room 后，将本对象改为 `abstract class RoomDatabase()`
 *      加 `@Database` 注解，并实现 `Room.databaseBuilder(...)` 单例，
 *      `profileDao()` 改为 `abstract fun profileDao(): SpoofProfileDao`。
 *   3. 调用方（DeviceSpoofViewModel.profilesFlow）无需修改。
 */
object SpoofDatabase {

    @Volatile private var dao: SpoofProfileDao? = null

    fun get(context: Context): SpoofDatabase = this

    fun profileDao(): SpoofProfileDao =
        dao ?: synchronized(this) {
            dao ?: InMemorySpoofProfileDao().also { dao = it }
        }
}
