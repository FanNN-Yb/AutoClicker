package com.example.autoclicker

import androidx.compose.runtime.Stable
import org.json.JSONArray
import org.json.JSONObject

@Stable
data class MacroConfig(
    val name: String,
    val positions: List<Triple<Int, Int, Long>>,
    val speed: Float = 1.0f,
    val repeatCount: Int = 1
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        put("speed", speed.toDouble())
        put("repeatCount", repeatCount)
        val posArray = JSONArray()
        positions.forEach { (x, y, t) ->
            posArray.put(JSONObject().apply {
                put("x", x); put("y", y); put("t", t)
            })
        }
        put("positions", posArray)
    }

    companion object {
        fun fromJson(obj: JSONObject): MacroConfig {
            val name = obj.optString("name", "未命名")
            val speed = obj.optDouble("speed", 1.0).toFloat()
            val repeatCount = obj.optInt("repeatCount", 1)
            val posArray = obj.optJSONArray("positions") ?: JSONArray()
            val positions = (0 until posArray.length()).mapNotNull { i ->
                try {
                    val p = posArray.getJSONObject(i)
                    Triple(p.optInt("x"), p.optInt("y"), p.optLong("t"))
                } catch (_: Exception) {
                    null // 跳过损坏的位置数据
                }
            }
            return MacroConfig(name, positions, speed, repeatCount)
        }
    }
}
