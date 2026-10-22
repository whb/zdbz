package com.zdbz.trucknavi.model

import android.content.Context
import android.content.SharedPreferences

/**
 * 货车参数：用于高德货车算路（AMapCarInfo）。
 * 用户在界面设置一次后持久化到 SharedPreferences。
 */
data class TruckInfo(
    var plate: String = "",          // 车牌号
    var carSize: Int = SIZE_HEAVY,   // 车型 1-4
    var length: String = "12",       // 长度(米)
    var width: String = "2.5",       // 宽度(米)
    var height: String = "4",        // 高度(米)
    var weight: String = "10",       // 核定载重(吨)
    var load: String = "20",         // 总重(吨)
    var axis: String = "2",          // 轴数
    var avoidRestrict: Boolean = true, // 躲避限行
    var loadSwitch: Boolean = true      // 载重参与算路
) {
    companion object {
        const val SIZE_MICRO = 1   // 微型货车
        const val SIZE_LIGHT = 2   // 轻型/小型货车
        const val SIZE_MEDIUM = 3  // 中型货车
        const val SIZE_HEAVY = 4   // 重型货车

        private const val PREF = "truck_navi"

        fun load(ctx: Context): TruckInfo {
            val sp: SharedPreferences = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            return TruckInfo(
                plate = sp.getString("plate", "") ?: "",
                carSize = sp.getInt("carSize", SIZE_HEAVY),
                length = sp.getString("length", "12") ?: "12",
                width = sp.getString("width", "2.5") ?: "2.5",
                height = sp.getString("height", "4") ?: "4",
                weight = sp.getString("weight", "10") ?: "10",
                load = sp.getString("load", "20") ?: "20",
                axis = sp.getString("axis", "2") ?: "2",
                avoidRestrict = sp.getBoolean("avoidRestrict", true),
                loadSwitch = sp.getBoolean("loadSwitch", true)
            )
        }

        fun save(ctx: Context, info: TruckInfo) {
            ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                .putString("plate", info.plate)
                .putInt("carSize", info.carSize)
                .putString("length", info.length)
                .putString("width", info.width)
                .putString("height", info.height)
                .putString("weight", info.weight)
                .putString("load", info.load)
                .putString("axis", info.axis)
                .putBoolean("avoidRestrict", info.avoidRestrict)
                .putBoolean("loadSwitch", info.loadSwitch)
                .apply()
        }
    }
}
