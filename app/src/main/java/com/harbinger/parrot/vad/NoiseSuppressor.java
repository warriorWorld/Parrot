package com.harbinger.parrot.vad;


import android.util.Log;

import com.harbinger.parrot.config.RooboServiceConfig;

import vip.inode.demo.webrtc.NoiseSuppressorUtils;

/**
 * Created by acorn on 2022/11/28.
 */
public class NoiseSuppressor {
    private final String TAG = "NoiseSuppressor";
    private static volatile NoiseSuppressor instance;

    public static NoiseSuppressor getInstance() {
        if (instance == null) {
            synchronized (NoiseSuppressor.class) {
                if (instance == null) {
                    instance = new NoiseSuppressor();
                }
            }
        }
        return instance;
    }

    private long nsHandler = 0;
    private final NoiseSuppressorUtils noiseSuppressorUtils = new NoiseSuppressorUtils();

    private NoiseSuppressor() {
    }

    public void init() {
        Log.d(TAG, "ns init:" + nsHandler);
        try {
            nsHandler = noiseSuppressorUtils.nsxCreate();
            noiseSuppressorUtils.nsxInit(nsHandler, RooboServiceConfig.INSTANCE.getAudioSampleRateInHz());
            noiseSuppressorUtils.nsxSetPolicy(nsHandler, 2);
        } catch (Exception e) {
            Log.d(TAG, "ns init exception:" + e.getMessage());
        }
    }

    public short[] process(short[] input) {
        short[] output = new short[input.length];
        try {
            noiseSuppressorUtils.nsxProcess(nsHandler, input, 1, output);
            Log.v(TAG, "ns process(" + nsHandler + "):" + input.length + "," + output.length);
            return output;
        } catch (Exception e) {
            Log.d(TAG, "ns process exception:" + e.getMessage());
            release();
            init();
        }
        return output;
    }

    public void release() {
        Log.d(TAG, "ns release:" + nsHandler);

        if (nsHandler != 0) {
            try {
                noiseSuppressorUtils.nsxFree(nsHandler);
            } catch (Exception e) {
                Log.d(TAG, "ns release exception:" + e.getMessage());
            }
            nsHandler = 0;
        }
    }
}
