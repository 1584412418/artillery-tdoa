package com.artillery.tdoa

class TdoaSolver {
    data class Station(val x:Double, val y:Double, val ts:Long)
    private val stations = mutableListOf<Station>()
    private val soundSpeed = 340.0

    fun addStation(x:Double,y:Double,ts:Long) {
        stations.add(Station(x,y,ts))
        if(stations.size>4) stations.removeAt(0)
    }

    fun solve():Pair<Double,Double>? {
        if(stations.size<3) return null
        // 当前占位，后面替换高斯牛顿TDOA解算
        val s = stations[0]
        return Pair(s.x+10, s.y+10)
    }
}
