package com.harbinger.parrot.vad

import android.content.Context
import com.harbinger.parrot.utils.FileUtil
import com.konovalov.vad.VadConfig
import java.io.File

/**
 * Created by acorn on 2020/11/20.
 */
class VadRecorder(context: Context) : IVADRecorder {
    private var listener: VADListener? = null
    private var isSpeaking = false
    private var voiceRecorder: VoiceRecorder? = null

    init {
        voiceRecorder = VoiceRecorder(
            context,
            object : VoiceRecorder.Listener {
                override fun onSpeechDetected() {
                    if (!isSpeaking) {
                        isSpeaking = true
                        listener?.onBos()
                    }
                }

                override fun onNoiseDetected() {
                    if (isSpeaking) {
                        isSpeaking = false
                        val pcmPath = voiceRecorder?.recordPath
                        pcmPath?.let {
                            val wavPath = pcmPath.replace(".pcm", ".wav")
                            FileUtil.savePcmToWav(File(pcmPath), File(wavPath))
                            FileUtil.deleteFile(File(pcmPath))
                            listener?.onEos(it)
                        }
                    }
                }
            }, VadConfig.newBuilder()
                .setSampleRate(VadConfig.SampleRate.SAMPLE_RATE_16K)
                .setFrameSize(VadConfig.FrameSize.FRAME_SIZE_160)
                .setMode(VadConfig.Mode.VERY_AGGRESSIVE)
                .setSilenceDurationMillis(500)
                .setVoiceDurationMillis(500)
                .build()
        )
    }

    override fun setVadListener(listener: VADListener) {
        this.listener = listener
    }

    override fun start() {
        voiceRecorder?.start()
    }

    override fun stop() {
        voiceRecorder?.stop()
    }
}