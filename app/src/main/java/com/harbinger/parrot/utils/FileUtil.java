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
import java.io.InputStream;
import java.io.OutputStream;

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
        String pcmDirectoryPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "Bat" + File.separator;
        File pcmDirectory = new File(pcmDirectoryPath);
        if (!pcmDirectory.exists()) {
            pcmDirectory.mkdirs();
        }
        return pcmDirectoryPath;
    }

    public static File getPermanentRecordDirectory() {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator +
                "Bat" + File.separator + "Permanent" + File.separator;
        File directory = new File(path);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return directory;
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

    public static String getSize(long bytes) {
        if (bytes < 1024) {
            return bytes + "B";
        } else if (bytes < 1024 * 1024) {
            return (bytes / 1024) + "KB";
        } else if (bytes < 1024 * 1024 * 1024) {
            return (bytes / 1024 / 1024) + "MB";
        } else {
            return (bytes / 1024 / 1024 / 1024) + "GB";
        }
    }


    /**
     * 复制文件目录
     *
     * @param srcDir  要复制的源目录 eg:/mnt/sdcard/DB
     * @param destDir 复制到的目标目录 eg:/mnt/sdcard/db/
     * @return
     */
    public static boolean copyDir(String srcDir, String destDir) {
        File sourceDir = new File(srcDir);
        //判断文件目录是否存在
        if (!sourceDir.exists()) {
            return false;
        }
        //判断是否是目录
        if (sourceDir.isDirectory()) {
            File[] fileList = sourceDir.listFiles();
            File targetDir = new File(destDir);
            //创建目标目录
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }
            //遍历要复制该目录下的全部文件
            for (int i = 0; i < fileList.length; i++) {
                if (fileList[i].isDirectory()) {//如果如果是子目录进行递归
                    copyDir(fileList[i].getPath() + "/",
                            destDir + fileList[i].getName() + "/");
                } else {//如果是文件则进行文件拷贝
                    copyFile(fileList[i].getPath(), destDir + fileList[i].getName());
                }
            }
            return true;
        } else {
            copyFileToDir(srcDir, destDir);
            return true;
        }
    }

    /**
     * 把文件拷贝到某一目录下
     *
     * @param srcFile
     * @param destDir
     * @return
     */
    public static boolean copyFileToDir(String srcFile, String destDir) {
        File fileDir = new File(destDir);
        if (!fileDir.exists()) {
            fileDir.mkdir();
        }
        String destFile = destDir + "/" + new File(srcFile).getName();
        try {
            InputStream streamFrom = new FileInputStream(srcFile);
            OutputStream streamTo = new FileOutputStream(destFile);
            byte buffer[] = new byte[1024];
            int len;
            while ((len = streamFrom.read(buffer)) > 0) {
                streamTo.write(buffer, 0, len);
            }
            streamFrom.close();
            streamTo.close();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * 复制文件（非目录）
     *
     * @param srcFile  要复制的源文件
     * @param destFile 复制到的目标文件
     * @return
     */
    public static boolean copyFile(String srcFile, String destFile) {
        try {
            InputStream streamFrom = new FileInputStream(srcFile);
            OutputStream streamTo = new FileOutputStream(destFile);
            byte buffer[] = new byte[1024];
            int len;
            while ((len = streamFrom.read(buffer)) > 0) {
                streamTo.write(buffer, 0, len);
            }
            streamFrom.close();
            streamTo.close();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * 移动文件目录到某一路径下
     *
     * @param srcFile
     * @param destDir
     * @return
     */
    public static boolean moveFile(String srcFile, String destDir) {
        //复制后删除原目录
        if (copyDir(srcFile, destDir)) {
            deleteFile(new File(srcFile));
            return true;
        }
        return false;
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

    public static void clearDirectoryExcept(File dir, File except) {
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
                if (except.isFile() && except.getPath().equals(file.getPath())) {
                    Log.d(TAG, "file is exception,so we'll ignore it");
                    continue;
                }
                Log.d(TAG, "delete " + file.getName());
                file.delete();
            } else {
                if (except.isDirectory() && except.getPath().equals(file.getPath())) {
                    Log.d(TAG, "directory is exception,so we'll ignore it");
                    continue;
                }
                clearDirectoryExcept(file, except);
            }
        }
    }
}
