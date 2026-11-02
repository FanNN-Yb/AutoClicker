package com.example.autoclicker

import com.tencent.mmkv.MMKV
import org.json.JSONArray

/**
 * 统一的宏配置持久化仓库，所有组件通过此类读写宏配置，
 * 消除 MacroConfigScreen / MacroDetailScreen / MacroDetailActivity 中的重复代码。
 *
 * ⚠️ 宏的**唯一标识就是 name**（不是 id）。所以：
 * - 改名必须带上原名，否则找不到要替换的那一条（[updateMacro]）；
 * - 写入前一定要过 [uniqueName]，重名会让列表的 LazyColumn key 冲突直接崩。
 */
object MacroRepository {

    private const val KEY = "configs"
    private const val FALLBACK_NAME = "未命名"
    private val mmkv: MMKV get() = MMKV.defaultMMKV()

    fun loadAll(): List<MacroConfig> {
        val json = mmkv.decodeString(KEY) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                try {
                    MacroConfig.fromJson(arr.getJSONObject(i))
                } catch (e: Exception) {
                    null // 跳过损坏的条目
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveAll(macros: List<MacroConfig>) {
        val arr = JSONArray()
        macros.forEach { arr.put(it.toJson()) }
        mmkv.encode(KEY, arr.toString())
    }

    /** 新增，名字自动去重。返回真正存进去的那条。 */
    fun addMacro(config: MacroConfig): MacroConfig {
        val macros = loadAll().toMutableList()
        val stored = config.copy(name = uniqueName(config.name))
        macros.add(stored)
        saveAll(macros)
        return stored
    }

    /**
     * 按**原名**替换一条宏。
     *
     * 不能用 `config.name` 查找：用户在详情页改名后，新名字在库里根本不存在，
     * 查找会返回 -1 然后静默跳过 —— 表现就是「改名 + 改参数后保存，什么都没变」。
     *
     * @return 实际存进去的配置（名字可能被自动编号），原名不存在时返回 null
     */
    fun updateMacro(originalName: String, config: MacroConfig): MacroConfig? {
        val macros = loadAll().toMutableList()
        val index = macros.indexOfFirst { it.name == originalName }
        if (index < 0) return null
        val stored = config.copy(name = uniqueName(config.name, excluding = originalName))
        macros[index] = stored
        saveAll(macros)
        return stored
    }

    fun deleteMacro(name: String) {
        val macros = loadAll().toMutableList()
        macros.removeAll { it.name == name }
        saveAll(macros)
    }

    fun findByName(name: String): MacroConfig? {
        return loadAll().find { it.name == name }
    }

    /**
     * 生成不重名的名字：已存在就追加 " (2)"、" (3)"…
     *
     * @param excluding 改名场景传自己的原名，否则「改成同名」会被自己挡下来编号
     */
    fun uniqueName(base: String, excluding: String? = null): String {
        val trimmed = base.trim().ifEmpty { FALLBACK_NAME }
        val taken = loadAll().mapTo(mutableSetOf()) { it.name }
        excluding?.let { taken.remove(it) }
        if (trimmed !in taken) return trimmed
        var i = 2
        while ("$trimmed ($i)" in taken) i++
        return "$trimmed ($i)"
    }
}
