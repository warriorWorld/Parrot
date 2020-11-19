package com.harbinger.parrot.vad

/**
 * Created by acorn on 2020/11/19.
 */
interface IVADRecorder {
    fun start(listener: VADListener)
    fun stop()
}