package com.harbinger.parrot.vad

/**
 * Created by acorn on 2020/11/19.
 */
interface VADEventListener {
    fun onSpeakDetected()//每段音频调用一次
    fun onBos()
    fun onEos(valid:Boolean)//valid：音频是否有效
    fun onBuffer(buffer: ShortArray)
}