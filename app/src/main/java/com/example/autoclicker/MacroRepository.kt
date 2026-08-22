package com.example.autoclicker

import com.tencent.mmkv.MMKV
import org.json.JSONArray

/**
 * 统一的宏配置持久化仓库，所有组件通过此类读写宏配置，
 * 消除 MacroConfigScreen / MacroDetailScreen / MacroDetailActivity 中的重复代码。
 */
object MacroRepository {

    private const val KEY = "configs"
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

    fun addMacro(config: MacroConfig) {
        val macros = loadAll().toMutableList()
        macros.add(config)
        saveAll(macros)
    }

    fun updateMacro(config: MacroConfig) {
        val macros = loadAll().toMutableList()
        val index = macros.indexOfFirst { it.name == config.name }
        if (index >= 0) {
            macros[index] = config
            saveAll(macros)
        }
    }

    fun deleteMacro(name: String) {
        val macros = loadAll().toMutableList()
        macros.removeAll { it.name == name }
        saveAll(macros)
    }

    fun findByName(name: String): MacroConfig? {
        return loadAll().find { it.name == name }
    }
}
