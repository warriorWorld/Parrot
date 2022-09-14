package com.harbinger.parrot.vad

import android.content.Context
import android.util.Log
import com.harbinger.parrot.config.ShareKeys
import com.harbinger.parrot.utils.CommonUtil
import com.harbinger.parrot.utils.FileUtil
import com.harbinger.parrot.utils.SharedPreferencesUtils
import com.konovalov.vad.VadConfig
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

/**
 * Created by acorn on 2020/11/20.
 * even have a VAD,but still record all voice.
 */
class VadButRecordAllRecorder(
    context: Context,
    directoryPath: String,
    silenceDuration: Int,
    speechDuration: Int
) : IVADRecorder {
    private val TAG = "VadButRecordAllRecorder"
    private var listener: VADListener? = null
    private var isSpeaking = false
    private var isContinousSpeaking = false
    private var voiceRecorder: VoiceRecorder? = null
    private var fos: FileOutputStream? = null
    private var recordPath: String? = null

    init {
        try {
            recordPath = directoryPath + FileUtil.getWritablePcmName(context)
            fos = FileOutputStream(recordPath, true)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        voiceRecorder = VoiceRecorder(
            context,
            object : VoiceRecorder.Listener {
                override fun onSpeechDetected() {
                    if (!isSpeaking) {
                        isSpeaking = true
                    }
                }

                override fun onContinuousSpeechDetected() {
                    if (!isContinousSpeaking) {
                        isContinousSpeaking = true
                        Log.d(TAG, "on bos")
                        listener?.onBos()
                    }
                }

                override fun onContinuousNoiseDetected() {
                    if (isContinousSpeaking) {
                        isSpeaking = false
                        isContinousSpeaking = false
                        Log.d(TAG, "on eos")
                        recordPath?.let { listener?.onEos(it) }
                    } else {
                        if (isSpeaking) {
                            Log.d(TAG, "dump speaking")
                            isSpeaking = false
                        }
                    }
                }

                override fun onBuffer(buffer: ShortArray?) {
                    if (fos != null) {
                        val bytes = CommonUtil.shortToBytes(buffer)
                        try {
                            Log.d(TAG, "write to pcm")
                            fos!!.write(bytes)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
            }, VadConfig.newBuilder()
                .setSampleRate(VadConfig.SampleRate.SAMPLE_RATE_48K)
                .setFrameSize(VadConfig.FrameSize.FRAME_SIZE_1440)
                .setMode(VadConfig.Mode.VERY_AGGRESSIVE)
                .setSilenceDurationMillis(silenceDuration)
                .setVoiceDurationMillis(speechDuration)
                .build()
        )
    }

    override fun setVadListener(listener: VADListener) {
        this.listener = listener
    }

    override fun start() {
        voiceRecorder?.start()
    }

    private fun closeFos() {
        if (null != fos) {
            try {
                fos!!.close()
                fos = null
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    override fun stop() {
        voiceRecorder?.stop()
        val pcmPath = recordPath
        pcmPath?.let {
            val wavPath = pcmPath.replace(".pcm", ".wav")
            FileUtil.savePcmToWav(File(pcmPath), File(wavPath))
            FileUtil.deleteFile(File(pcmPath))
        }
        closeFos()
    }
}