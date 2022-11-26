package com.harbinger.parrot.vad;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.NoiseSuppressor;
import android.util.Log;

import com.konovalov.vad.Vad;
import com.konovalov.vad.VadConfig;
import com.konovalov.vad.VadListener;

import static android.media.AudioFormat.CHANNEL_IN_MONO;
import static android.media.AudioFormat.CHANNEL_IN_STEREO;

import vip.inode.demo.webrtc.NoiseSuppressorUtils;

/**
 * Created by George Konovalov on 11/16/2019.
 */

public class VoiceRecorder {
    private static final int PCM_CHANNEL = CHANNEL_IN_MONO;
    private static final int PCM_ENCODING_BIT = AudioFormat.ENCODING_PCM_16BIT;
    private Context context;
    private Vad vad;
    private AudioRecord audioRecord;
    private Listener callback;
    private Thread thread;

    private boolean isListening = false;
    private static final String TAG = "VoiceRecorder";
    //降噪
    private long nsHandler;
    private NoiseSuppressorUtils noiseSuppressorUtils = new NoiseSuppressorUtils();

    public VoiceRecorder(Context context, Listener callback, VadConfig config) {
        this.context = context;
        this.callback = callback;
        this.vad = new Vad(config);
    }

    public void updateConfig(VadConfig config) {
        vad.setConfig(config);
    }

    public void start() {
        stop();
        nsHandler = noiseSuppressorUtils.nsxCreate();
        noiseSuppressorUtils.nsxInit(nsHandler, vad.getConfig().getSampleRate().getValue());
        noiseSuppressorUtils.nsxSetPolicy(nsHandler, 2);
        audioRecord = createAudioRecord();
        if (audioRecord != null) {
            isListening = true;
            audioRecord.startRecording();

            thread = new Thread(new ProcessVoice());
            thread.start();
            vad.start();
        } else {
            Log.w(TAG, "Failed start Voice Recorder!");
        }
    }


    public void stop() {
        isListening = false;
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
        if (nsHandler > 0) {
            noiseSuppressorUtils.nsxFree(nsHandler);
        }
        if (audioRecord != null) {
            try {
                audioRecord.release();
            } catch (Exception e) {
                Log.e(TAG, "Error stop AudioRecord ", e);
            }
            audioRecord = null;
        }
        if (vad != null) {
            vad.stop();
        }
    }


    private AudioRecord createAudioRecord() {
        try {
            final int minBufSize = AudioRecord.getMinBufferSize(vad.getConfig().getSampleRate().getValue(), PCM_CHANNEL, PCM_ENCODING_BIT);

            if (minBufSize == AudioRecord.ERROR_BAD_VALUE) {
                return null;
            }

            final AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, vad.getConfig().getSampleRate().getValue(), PCM_CHANNEL, PCM_ENCODING_BIT, minBufSize);
            if (NoiseSuppressor.isAvailable()) {
                NoiseSuppressor noiseSuppressor = NoiseSuppressor.create(audioRecord.getAudioSessionId());
                noiseSuppressor.setEnabled(true);
            }

            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                return audioRecord;
            } else {
                audioRecord.release();
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Error can't create AudioRecord ", e);
        }

        return null;
    }

    private int getNumberOfChannels() {
        switch (PCM_CHANNEL) {
            case CHANNEL_IN_MONO:
                return 1;
            case CHANNEL_IN_STEREO:
                return 2;
        }
        return 1;
    }

    private class ProcessVoice implements Runnable {

        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

            while (!Thread.interrupted() && isListening && audioRecord != null) {
                short[] buffer = new short[vad.getConfig().getFrameSize().getValue() * getNumberOfChannels()];
                short[] bufferDenoised = new short[buffer.length];

                audioRecord.read(buffer, 0, buffer.length);
                noiseSuppressorUtils.nsxProcess(nsHandler, buffer, 1, bufferDenoised);

                boolean isSpeech = vad.isSpeech(bufferDenoised);
                if (isSpeech) {
                    callback.onSpeechDetected();
                }
                isSpeechDetected(bufferDenoised);
                callback.onBuffer(bufferDenoised);
            }
        }

        private void isSpeechDetected(short[] buffer) {
            vad.isContinuousSpeech(buffer, new VadListener() {
                @Override
                public void onSpeechDetected() {
                    callback.onContinuousSpeechDetected();
                }

                @Override
                public void onNoiseDetected() {
                    callback.onContinuousNoiseDetected();
                }
            });
        }
    }

    public interface Listener {
        void onSpeechDetected();

        void onContinuousSpeechDetected();

        void onContinuousNoiseDetected();

        void onBuffer(short[] buffer);
    }
}
