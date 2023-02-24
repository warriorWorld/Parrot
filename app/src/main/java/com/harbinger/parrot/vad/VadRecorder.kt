package com.harbinger.parrot.vad

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process
import android.util.Log
import com.harbinger.parrot.config.RooboServiceConfig
import com.konovalov.vad.Vad
import com.konovalov.vad.VadConfig
import com.konovalov.vad.VadConfig.*
import com.konovalov.vad.VadListener

/**
 * Created by George Konovalov on 11/16/2019.
 */
class VadRecorder {
    private val vad: Vad?
    private var audioRecord: AudioRecord? = null
    private var thread: Thread? = null
    private var isListening = false

    init {
        vad = Vad(
            VadConfig.newBuilder()
                .setSampleRate(getVadSampleRate(RooboServiceConfig.audioSampleRateInHz))
                .setFrameSize(getVadFrameSize(RooboServiceConfig.audioFrameSize))
                .setMode(getVadMode(RooboServiceConfig.vadMode))
                .setSilenceDurationMillis(RooboServiceConfig.silenceDurationMillis)
                .setVoiceDurationMillis(RooboServiceConfig.voiceDurationMillis)
                .build()
        )
        NoiseSuppressor.getInstance().init()
    }

    private fun getVadSampleRate(sampleRate: Int): SampleRate {
        return when (sampleRate) {
            8000 -> SampleRate.SAMPLE_RATE_8K
            32000 -> SampleRate.SAMPLE_RATE_32K
            48000 -> SampleRate.SAMPLE_RATE_48K
            16000 -> SampleRate.SAMPLE_RATE_16K
            else -> SampleRate.SAMPLE_RATE_16K
        }
    }

    private fun getVadFrameSize(frameSize: Int): FrameSize {
        return when (frameSize) {
            80 -> FrameSize.FRAME_SIZE_80
            160 -> FrameSize.FRAME_SIZE_160
            240 -> FrameSize.FRAME_SIZE_240
            320 -> FrameSize.FRAME_SIZE_320
            480 -> FrameSize.FRAME_SIZE_480
            640 -> FrameSize.FRAME_SIZE_640
            960 -> FrameSize.FRAME_SIZE_960
            1440 -> FrameSize.FRAME_SIZE_1440
            else -> FrameSize.FRAME_SIZE_640
        }
    }

    private fun getVadMode(mode: Int): VadConfig.Mode {
        var finalMode = mode
        if (finalMode < 0) {
            finalMode = 0
        }
        if (finalMode > 3) {
            finalMode = 3
        }
        return when (finalMode) {
            0 -> Mode.NORMAL
            1 -> Mode.LOW_BITRATE
            2 -> Mode.AGGRESSIVE
            3 -> Mode.VERY_AGGRESSIVE
            else -> Mode.AGGRESSIVE
        }
    }

    fun updateConfig(config: VadConfig?) {
        vad!!.config = config
    }

    fun start(listener: VADEventListener) {
        stop()
        audioRecord = createAudioRecord()
        if (audioRecord != null) {
            isListening = true
            audioRecord!!.startRecording()
            thread = Thread(ProcessVoice(listener))
            thread!!.start()
            vad!!.start()
        } else {
            Log.w(TAG, "Failed start Voice Recorder!")
        }
    }

    fun stop() {
        isListening = false
        if (thread != null) {
            thread!!.interrupt()
            thread = null
        }
        if (audioRecord != null) {
            try {
                audioRecord!!.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error stop AudioRecord:${e.message}")
            }
            audioRecord = null
        }
        vad?.stop()
    }

    private fun createAudioRecord(): AudioRecord? {
        try {
            val minBufSize = AudioRecord.getMinBufferSize(
                RooboServiceConfig.audioSampleRateInHz,
                RooboServiceConfig.audioChannelConfig,
                RooboServiceConfig.audioFormat
            )
            if (minBufSize == AudioRecord.ERROR_BAD_VALUE) {
                return null
            }
            @SuppressLint("MissingPermission") val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                RooboServiceConfig.audioSampleRateInHz,
                RooboServiceConfig.audioChannelConfig,
                RooboServiceConfig.audioFormat,
                minBufSize
            )
            if (audioRecord.state == AudioRecord.STATE_INITIALIZED) {
                return audioRecord
            } else {
                audioRecord.release()
            }
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Error can't create AudioRecord:${e.message}")
        }
        return null
    }

    private val numberOfChannels: Int
        private get() {
            when (RooboServiceConfig.audioChannelConfig) {
                AudioFormat.CHANNEL_IN_MONO -> return 1
                AudioFormat.CHANNEL_IN_STEREO -> return 2
            }
            return 1
        }

    private inner class ProcessVoice(private val mVADEventListener: VADEventListener) : Runnable {
        private var isSpeaking = false
        private var isContinousSpeaking = false
        override fun run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            while (!Thread.interrupted() && isListening && audioRecord != null) {
                val buffer = ShortArray(RooboServiceConfig.audioFrameSize)
//                var bufferDenoised = ShortArray(buffer.size)

                audioRecord!!.read(buffer, 0, buffer.size)
//                bufferDenoised=buffer
                val bufferDenoised = NoiseSuppressor.getInstance().process(buffer)

                val isSpeech = vad!!.isSpeech(bufferDenoised)
                if (isSpeech) {
                    if (!isSpeaking) {
                        isSpeaking = true
                        mVADEventListener.onSpeakDetected()
                    }
                }
                isSpeechDetected(bufferDenoised)
                mVADEventListener.onBuffer(bufferDenoised)
            }
        }

        private fun isSpeechDetected(buffer: ShortArray) {
            vad!!.isContinuousSpeech(buffer, object : VadListener {
                override fun onSpeechDetected() {
                    if (!isContinousSpeaking) {
                        isContinousSpeaking = true
                        Log.d(TAG, "on bos")
                        mVADEventListener.onBos()
                    }
                }

                override fun onNoiseDetected() {
                    if (isContinousSpeaking) {
                        isSpeaking = false
                        isContinousSpeaking = false
                        Log.d(TAG, "on eos")
                        mVADEventListener.onEos(true)
                    } else {
                        if (isSpeaking) {
                            Log.d(TAG, "dump speaking")
                            isSpeaking = false
                            mVADEventListener.onEos(false)
                        }
                    }
                }
            })
        }
    }

    companion object {
        private const val TAG = "VoiceRecorder"
    }
}