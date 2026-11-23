package com.artillery.tdoa

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.nio.charset.StandardCharsets

class MasterUdpListener(private val port:Int) {
    var onLocationResult: ((Pair<Double,Double>)->Unit)? = null
    private var socket: DatagramSocket? = null
    private var thread:Thread? = null
    private var run = false
    private val solver = TdoaSolver()
    private val kalman = Kalman2D()

    fun start() {
        run = true
        socket = DatagramSocket(port)
        thread = Thread {
            val buf = ByteArray(1024)
            while (run) {
                val pkt = DatagramPacket(buf,buf.size)
                socket?.receive(pkt)
                val str = String(pkt.data,0,pkt.length, StandardCharsets.UTF_8)
                // 格式示例：SLAVE_ID,X,Y,TIMESTAMP
                val sp = str.split(",")
                if(sp.size>=4) {
                    val x = sp[1].toDoubleOrNull() ?: continue
                    val y = sp[2].toDoubleOrNull() ?: continue
                    val ts = sp[3].toLongOrNull() ?: continue
                    solver.addStation(x,y,ts)
                    val res = solver.solve()
                    if(res!=null) {
                        val smooth = kalman.update(res.first,res.second)
                        onLocationResult?.invoke(smooth)
                    }
                }
            }
        }
        thread?.start()
    }

    fun stop() {
        run = false
        socket?.close()
        thread?.interrupt()
    }
}
