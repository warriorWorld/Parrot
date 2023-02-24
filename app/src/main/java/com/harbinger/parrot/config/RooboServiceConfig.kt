package com.harbinger.parrot.config

import android.media.AudioFormat
import android.os.Environment
import java.io.File
import java.io.Serializable

object VADMode {
    var NORMAL = 0
    var LOW_BITRATE = 1
    var AGGRESSIVE = 2
    var VERY_AGGRESSIVE = 3
}

/**
 * Created by acorn on 2022/3/28.
 */
object RooboServiceConfig : Serializable {
    var clientId = ""
    var logLevel = 5//0:none,1:error,2:warn,3:info,4:debug,5:verbose
    var configPath = ""
    var pcmPath =
        Environment.getExternalStorageDirectory().absolutePath + File.separator + "Parrot" + File.separator;//cache PCM save path
    var persistedPcmPath =
        Environment.getExternalStorageDirectory().absolutePath + File.separator + "Bat" + File.separator//persisted PCM path use to test
    var token = "156ace54ac4e9108d0553eb44e895d9a7866"
    var agentId = "zBlYzc2ODI4NzBjN"
    var online = true

    //<<<recorder
    var audioSampleRateInHz = 16000
    var audioChannelConfig = AudioFormat.CHANNEL_IN_MONO
    var audioFormat = AudioFormat.ENCODING_PCM_16BIT
    var audioFrameSize = 160

    //vad
    var vadMode = VADMode.VERY_AGGRESSIVE//NORMAL(0),LOW_BITRATE(1),AGGRESSIVE(2),VERY_AGGRESSIVE(3)
    var silenceDurationMillis = 300
    var voiceDurationMillis = 300
}