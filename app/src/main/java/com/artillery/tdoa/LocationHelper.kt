package com.artillery.tdoa

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle

class LocationHelper(private val ctx: Context, private val zone: Int) {
    var onGaussResult: ((x:Double,y:Double)->Unit)? = null
    var onError: ((msg:String)->Unit)? = null
    private var lm: LocationManager? = null
    private var listener: LocationListener? = null

    fun start() {
        lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val (gx,gy) = GaussConvert.wgs84ToGauss(location.longitude, location.latitude, zone)
                onGaussResult?.invoke(gx,gy)
            }
            override fun onProviderDisabled(provider: String) {
                onError?.invoke("GPS关闭")
            }
            override fun onProviderEnabled(provider: String) {}
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }
        try {
            lm?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0f, listener!!)
        }catch (e:SecurityException){
            onError?.invoke("定位权限不足")
        }
    }

    fun stop() {
        listener?.let { lm?.removeUpdates(it) }
    }
}
