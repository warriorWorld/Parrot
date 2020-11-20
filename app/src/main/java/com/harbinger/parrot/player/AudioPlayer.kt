package com.harbinger.parrot.player

import android.media.MediaPlayer

/**
 * Created by acorn on 2020/11/20.
 */
class AudioPlayer() : IAudioPlayer {
    private var mediaPlayer: MediaPlayer? = null
    private var playListener: PlayListener? = null

    init {
        mediaPlayer = MediaPlayer()
        mediaPlayer?.setOnCompletionListener {
            playListener?.onComplete()
        }
    }

    override fun setPlayListener(playListener: PlayListener) {
        this.playListener = playListener
    }

    override fun play(path: String) {
        if (mediaPlayer?.isPlaying!!) {
            mediaPlayer?.stop()
        }
        mediaPlayer?.setDataSource(path)
        mediaPlayer?.prepare()
        mediaPlayer?.start()
        playListener?.onBegin()
    }

    override fun stop() {
        mediaPlayer?.stop()
    }
}