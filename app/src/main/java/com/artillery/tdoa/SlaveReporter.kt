package com.artillery.tdoa

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.charset.StandardCharsets

class SlaveReporter(private val masterIp:String, private val port:Int, private val id:String) {
    private var socket: DatagramSocket? = null
    private var thread:Thread? = null
    private var run = false
    private var destIp: InetAddress? = null

    fun start() {
        run = true
        socket = DatagramSocket()
        destIp = InetAddress.getByName(masterIp)
    }

    fun reportTrigger(x:Double,y:Double,ts:Long) {
        if(!run) return
        val msg = "$id,$x,$y,$ts"
        val data = msg.toByteArray(StandardCharsets.UTF_8)
        val pkt = DatagramPacket(data, data.size, destIp, port)
        socket?.send(pkt)
    }

    fun stop() {
        run = false
        socket?.close()
    }
}
