package com.harbinger.parrot.player

/**
 * Created by acorn on 2020/11/19.
 */
interface IAudioPlayer {
    fun setPlayListener(playListener: PlayListener)
    fun play(path: String)
    fun stop()
}