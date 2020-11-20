package com.harbinger.parrot.player

import android.content.Context

/**
 * Created by acorn on 2020/11/20.
 */
class AudioPlayer(context: Context) : IAudioPlayer {
    private var player: OncePlayer? = null
    private var playListener: PlayListener? = null

    init {
        player = OncePlayer.create(context)
    }

    override fun setPlayListener(playListener: PlayListener) {
        this.playListener = playListener
    }

    override fun play(path: String) {
        player?.play(
            path
        ) { playListener?.onComplete() }
        playListener?.onBegin()
    }

    override fun stop() {
        player?.stop()
    }
}