package com.artillery.tdoa

import android.content.Context
import android.content.SharedPreferences

class StationPref(ctx: Context) {
    private val sp: SharedPreferences = ctx.getSharedPreferences("station_cfg", Context.MODE_PRIVATE)

    fun saveXY(x: Double, y: Double) {
        sp.edit()
            .putFloat("gx", x.toFloat())
            .putFloat("gy", y.toFloat())
            .apply()
    }

    fun getX(): Double {
        return sp.getFloat("gx", 0f).toDouble()
    }

    fun getY(): Double {
        return sp.getFloat("gy", 0f).toDouble()
    }
}
