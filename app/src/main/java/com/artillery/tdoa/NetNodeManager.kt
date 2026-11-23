package com.artillery.tdoa

class NetNodeManager {
    data class Node(val x:Double, val y:Double, val ts:Long)
    private val list = mutableListOf<Node>()

    fun add(node:Node) {
        list.add(node)
        if(list.size>8) list.removeAt(0)
    }
    fun getAll():List<Node> = list
}
