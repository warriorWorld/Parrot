package com.harbinger.parrot.vad

import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import com.harbinger.parrot.config.RooboServiceConfig
import com.harbinger.parrot.utils.CommonUtil
import com.harbinger.parrot.vad.VADEventListener
import java.io.File

/**
 * Created by acorn on 2022/10/24.
 */
class VadProcesser(
    private val pcmPath: String,
    private val bos: () -> Unit,
    private val eos: () -> Unit,
    private val gotOne: (File) -> Unit
) : VADEventListener {
    private var pcmSaver: PcmSaver? = null
    private var closed = false
    private val mH = Handler(Looper.getMainLooper())

    init {
        pcmSaver =
            PcmSaver(File(pcmPath))
    }

    override fun onSpeakDetected() {

    }

    override fun onBos() {
        mH.post {
            bos()
        }
    }

    override fun onEos(valid: Boolean) {
        mH.post { eos() }
        if (!valid) {
            pcmSaver?.deleteFile()
        } else {
            val pcm = pcmSaver?.pcm
            if (null != pcm) {
                closed = true
                gotOne(pcm)
                pcmSaver?.close()
            }
        }
    }

    override fun onBuffer(buffer: ShortArray) {
        if (closed) {
            return
        }
        pcmSaver?.write(CommonUtil.shortToBytes(buffer))
    }
}