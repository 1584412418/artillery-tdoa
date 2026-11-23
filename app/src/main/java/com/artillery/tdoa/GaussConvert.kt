package com.artillery.tdoa

class GaussConvert {
    companion object {
        fun wgs84ToGauss(lon: Double, lat: Double, zone: Int): Pair<Double,Double> {
            // 简易占位，后续替换成完整高斯正算
            return Pair(lon*10000, lat*10000)
        }
    }
}
