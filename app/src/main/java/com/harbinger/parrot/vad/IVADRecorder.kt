package com.harbinger.parrot.vad

/**
 * Created by acorn on 2020/11/19.
 */
interface IVADRecorder {
    fun setVadListener(listener: VADListener)
    fun start()
    fun stop()
}