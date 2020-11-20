package com.harbinger.parrot.vad

import android.content.Context
import android.util.Log
import com.harbinger.parrot.utils.CommonUtil
import com.harbinger.parrot.utils.FileUtil
import com.konovalov.vad.VadConfig
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

/**
 * Created by acorn on 2020/11/20.
 */
class VadRecorder(context: Context) : IVADRecorder {
    private val TAG="VadRecorder"
    private var listener: VADListener? = null
    private var isSpeaking = false
    private var voiceRecorder: VoiceRecorder? = null
    private var fos: FileOutputStream? = null
    private var recordPath: String? = null

    init {
        voiceRecorder = VoiceRecorder(
            context,
            object : VoiceRecorder.Listener {

                override fun onSpeechDetected(buffer: ShortArray?) {
                    if (!isSpeaking) {
                        Log.d(TAG,"on bos")
                        isSpeaking = true
                        listener?.onBos()
                        try {
                            recordPath = FileUtil.getWritablePcmPath(context)
                            fos = FileOutputStream(recordPath, true)
                        } catch (e: FileNotFoundException) {
                            e.printStackTrace()
                        }
                    }
                    if (fos != null) {
                        val bytes = CommonUtil.shortToBytes(buffer)
                        try {
                            Log.d(TAG,"write to pcm")
                            fos!!.write(bytes)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }

                override fun onNoiseDetected() {
                    if (isSpeaking) {
                        isSpeaking = false
                        val pcmPath = recordPath
                        pcmPath?.let {
                            Log.d(TAG,"on eos")
                            val wavPath = pcmPath.replace(".pcm", ".wav")
                            FileUtil.savePcmToWav(File(pcmPath), File(wavPath))
                            FileUtil.deleteFile(File(pcmPath))
                            listener?.onEos(wavPath)
                        }
                        closeFos()
                    }
                }
            }, VadConfig.newBuilder()
                .setSampleRate(VadConfig.SampleRate.SAMPLE_RATE_48K)
                .setFrameSize(VadConfig.FrameSize.FRAME_SIZE_1440)
                .setMode(VadConfig.Mode.VERY_AGGRESSIVE)
                .setSilenceDurationMillis(600)
                .setVoiceDurationMillis(10)
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
                fos=null
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    override fun stop() {
        voiceRecorder?.stop()
        closeFos()
    }
}