package com.harbinger.parrot.utils;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by acorn on 2020/11/20.
 */
public class FileUtil {
    private static final String TAG = "FileUtil";
    private static final int SAMPLE_RATE_INHZ = 48000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    public static String getTempRecordDirectory() {
        String pcmDirectoryPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "Parrot" + File.separator;
        File pcmDirectory = new File(pcmDirectoryPath);
        if (!pcmDirectory.exists()) {
            pcmDirectory.mkdirs();
        }
        return pcmDirectoryPath;
    }

    public static String getReservedRecordDirectory() {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "Bat" + File.separator;
    }

    public static String getWritablePcmName(Context context) {
        return System.currentTimeMillis() + ".pcm";
    }

    // return size of inFilename
    public static long savePcmToWav(File pcmFile, File wavFile) {
        Log.d(TAG, "savePcmToWav start");
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalPcmLen = 0;
        long totalWavLen;
        long longSampleRate = SAMPLE_RATE_INHZ;
        int channels = 1;
        long byteRate = 16 * SAMPLE_RATE_INHZ * channels / 8;
        byte[] data = new byte[AudioRecord.getMinBufferSize(SAMPLE_RATE_INHZ, CHANNEL_CONFIG, AUDIO_FORMAT)];
        try {
            in = new FileInputStream(pcmFile);
            totalPcmLen = in.getChannel().size();
            if (totalPcmLen == 0) {
                // inFilename is empty
                return 0;
            }
            out = new FileOutputStream(wavFile, true);
            totalWavLen = totalPcmLen + 36;
            writeWaveFileHeader(out, totalPcmLen, totalWavLen, longSampleRate, channels, byteRate);
            while (in.read(data) != -1) {
                out.write(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Log.d(TAG, "savePcmToWav finish");
        }
        return totalPcmLen;
    }

    /**
     * 加入wav文件头
     */
    private static void writeWaveFileHeader(FileOutputStream out, long totalAudioLen,
                                            long totalDataLen, long longSampleRate, int channels, long byteRate)
            throws IOException {
        byte[] header = new byte[44];
        // RIFF/WAVE header
        header[0] = 'R';
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        //WAVE
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        // 'fmt ' chunk
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        // 4 bytes: size of 'fmt ' chunk
        header[16] = 16;
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        // format = 1
        header[20] = 1;
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        // block align
        header[32] = (byte) (2 * 16 / 8);
        header[33] = 0;
        // bits per sample
        header[34] = 16;
        header[35] = 0;
        //data
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);
    }

    public static void deleteFile(File file) {
        if (file.isFile() && file.exists()) {
            file.delete();
            return;
        }
        if (file.isDirectory()) {
            File[] childFile = file.listFiles();
            if (childFile == null || childFile.length == 0) {
                file.delete();
                return;
            }
            for (File f : childFile) {
                deleteFile(f);
            }
            file.delete();
        }
    }

    public static void clearDirectory(File dir) {
        Log.d(TAG, "clear dir:" + dir);
        if (dir.isFile()) {
            Log.d(TAG, "clear dir is file");
            dir.delete();
            return;
        }
        File[] files = dir.listFiles();
        if (null == files || files.length == 0) {
            Log.d(TAG, "dir is empty");
            return;
        }
        for (File file : files) {
            if (file.isFile()) {
                Log.d(TAG, "delete " + file.getName());
                file.delete();
            } else {
                clearDirectory(file);
            }
        }
    }
}
