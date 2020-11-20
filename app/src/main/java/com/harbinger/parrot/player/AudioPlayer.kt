package com.harbinger.parrot.player

import android.content.Context

/**
 * Created by acorn on 2020/11/20.
 */
class AudioPlayer(val context: Context) : IAudioPlayer {
    private var player: OncePlayer? = null
    private var playListener: PlayListener? = null

    override fun setPlayListener(playListener: PlayListener) {
        this.playListener = playListener
    }

    override fun play(path: String) {
        player = OncePlayer.create(context)
        player?.play(
            path
        ) { playListener?.onComplete() }
        playListener?.onBegin()
    }

    override fun stop() {
        player?.stop()
    }
}