package com.harbinger.parrot.vad;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.harbinger.parrot.utils.FileUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by acorn on 2022/4/11.
 */
public class PcmSaver {
    private final String TAG = "PcmSaver";
    private FileOutputStream stream;
    private String filePath;
    private File saveDirector;
//    private Handler mHandler;
//    private boolean preparing = false;

    public PcmSaver(File saveDirector) {
        if (null == saveDirector) {
            Log.d(TAG, "save director is null");
            return;
        }
        if (!saveDirector.exists()) {
            saveDirector.mkdirs();
        }
        Log.d(TAG, "director path:" + saveDirector.getPath());
        this.saveDirector = saveDirector;
//        Looper.prepare();
//        mHandler = new Handler(Looper.myLooper());
        createNewStream();
    }

    private void createNewStream() {
        try {
//            preparing=true;
            filePath = saveDirector.getPath() + File.separator + System.currentTimeMillis() + ".pcm";
            Log.d(TAG, "create new stream:" + filePath);
            stream = new FileOutputStream(filePath);
//          mHandler.postDelayed(new Runnable() {
//              @Override
//              public void run() {
//                  preparing=false;
//              }
//          },50);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public File getPcm() {
        return new File(filePath);
    }

    public void deleteFile() {
        Log.d(TAG, "delete file:" + filePath);
        try {
            stream.close();
            FileUtil.deleteFile(new File(filePath));
            createNewStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void write(byte[] data) {
//        if (preparing){
//            Log.d(TAG,"in preparing..."+filePath);
//            return;
//        }
        //写入文件
        try {
            stream.write(data);
        } catch (IOException e) {
            Log.e(TAG, "stream write failed" + "(" + filePath + "):" + e.getMessage());
            e.printStackTrace();
        }
    }

    public void close() {
        if (null != stream) {
            Log.d(TAG,"pcm close called:"+filePath);
            try {
                stream.close();
            } catch (IOException e) {
                Log.e(TAG, "stream close failed:" + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
