package com.artillery.tdoa

class Kalman2D {
    private var x = 0.0
    private var y = 0.0
    private val q = 0.01
    private val r = 0.8
    private var p = arrayOf(doubleArrayOf(1.0,0.0), doubleArrayOf(0.0,1.0))

    fun update(nx:Double, ny:Double): Pair<Double,Double> {
        val k1 = p[0][0]/(p[0][0]+r)
        val k2 = p[1][1]/(p[1][1]+r)
        x += k1*(nx - x)
        y += k2*(ny - y)
        p[0][0] = (1-k1)*p[0][0] + q
        p[1][1] = (1-k2)*p[1][1] + q
        return Pair(x,y)
    }
}
