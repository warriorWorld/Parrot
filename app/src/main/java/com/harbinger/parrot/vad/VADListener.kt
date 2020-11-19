package com.harbinger.parrot.vad

/**
 * Created by acorn on 2020/11/19.
 */
interface VADListener {
    fun onBos()
    fun onEos(recordPath: String)
}