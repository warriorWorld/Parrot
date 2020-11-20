package com.harbinger.parrot.player;


import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;


import java.io.IOException;

/**
 * TODO: 简易播放组件, 暂时限制多一点, 仅限使用一次, 仅限同一线程调用, 没有暂停
 */
public final class OncePlayer
        implements
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnStateChangedListener,
        MediaPlayer.OnCompletionListener {

    private static final String TAG = "OncePlayer";

    private Context mContext;

    private final MediaPlayer mMediaPlayer;

    private boolean mUsed;

    private MediaPlayer.OnStateChangedListener mInnerListener;

    private Thread mRestrictThread;

    private Handler mH;

    public static OncePlayer create(Context context) {
        return new OncePlayer(context);
    }

    private OncePlayer(Context context) {
        mContext = context;
        mRestrictThread = Thread.currentThread();
        mH = new Handler(Looper.myLooper() != null ? Looper.myLooper() : Looper.getMainLooper());
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnStateChangedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
    }

    public boolean play(AssetFileDescriptor fd) {
        if (mRestrictThread != Thread.currentThread()) {
            throw new RuntimeException("Restrict Thread Exception");
        }
        boolean ret;
        if (mMediaPlayer != null) {
            if (!mUsed) {
                mUsed = true;
                // create temp file that will hold byte array
                if (mMediaPlayer.reset() != MediaPlayer.RET_SUCCESS) {
                    Log.w(TAG, "play: reset err. ignore here." + this);
                }
                int setDataSourceResult = mMediaPlayer.setDataSource(mContext, fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
                if (setDataSourceResult == MediaPlayer.RET_SUCCESS) {
                    int prepareResult = mMediaPlayer.prepare();
                    ret = prepareResult == MediaPlayer.RET_SUCCESS;
                } else {
                    ret = false;
                }
            } else {
                Log.d(TAG, "play started, return" + this);
                ret = true;
            }
        } else {
            Log.e(TAG, "play: maybe mMediaPlayer released." + this);
            ret = false;
        }
        if (!ret) {
            releaseLocked();
        }
        return ret;
    }

    public boolean play(String url) {
        if (mRestrictThread != Thread.currentThread()) {
            throw new RuntimeException("Restrict Thread Exception");
        }
        boolean ret;
        if (mMediaPlayer != null) {
            if (!mUsed) {
                mUsed = true;
                // create temp file that will hold byte array
                if (mMediaPlayer.reset() != MediaPlayer.RET_SUCCESS) {
                    Log.w(TAG, "play: reset err. ignore here. " + this);
                }
                int setDataSourceResult = mMediaPlayer.setDataSource(mContext, Uri.parse(url));
                if (setDataSourceResult == MediaPlayer.RET_SUCCESS) {
                    int prepareResult = mMediaPlayer.prepare();
                    if (prepareResult == MediaPlayer.RET_SUCCESS) {
                        Log.d(TAG, "play started, return. " + this);
                        // 5s超时
                        mH.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (mMediaPlayer.getCurrentState() == MediaPlayer.State.Preparing) {
                                    Log.e(TAG, "play: run: timeout!!!");
                                    releaseLocked();
                                }
                            }
                        }, 5000);
                        ret = true;
                    } else {
                        ret = false;
                    }
                } else {
                    ret = false;
                }
            } else {
                Log.d(TAG, "play started, return. " + this);
                ret = true;
            }
        } else {
            Log.e(TAG, "play: maybe mMediaPlayer released. " + this);
            ret = false;
        }
        if (!ret) {
            Log.d(TAG, "play: start err, then release. " + this);
            releaseLocked();
        }
        return ret;
    }

    public boolean play(String url, final OnFinishListener onFinishListener) {
        setOnStateChangedListener(new MediaPlayer.OnStateChangedListener() {
            @Override
            public void onStateChanged(MediaPlayer.State from, MediaPlayer.State to) {
                if (to == MediaPlayer.State.End ||
                        to == MediaPlayer.State.Error ||
                        to == MediaPlayer.State.PlaybackCompleted ||
                        to == MediaPlayer.State.Stopped) {
                    setOnStateChangedListener(null);
                    if (onFinishListener != null) {
                        onFinishListener.onFinish();
                    }
                }
            }
        });
        return play(url);
    }

    public boolean playAssetsFile(String assetsPath) {
        return playAssetsFile(assetsPath, null);
    }

    public boolean playAssetsFile(String assetsPath, final OnFinishListener onFinishListener) {
        final AssetFileDescriptor fd;
        try {
            fd = mContext.getAssets().openFd(assetsPath);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        setOnStateChangedListener(new MediaPlayer.OnStateChangedListener() {
            @Override
            public void onStateChanged(MediaPlayer.State from, MediaPlayer.State to) {
                if (to == MediaPlayer.State.End ||
                        to == MediaPlayer.State.Error ||
                        to == MediaPlayer.State.PlaybackCompleted ||
                        to == MediaPlayer.State.Stopped) {
                    setOnStateChangedListener(null);
                    if (onFinishListener != null) {
                        onFinishListener.onFinish();
                    }
                    try {
                        fd.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        });
        boolean ret = play(fd);
        if (!ret) {
            try {
                fd.close();
            } catch (IOException ignored) {
            }
        }
        return ret;
    }

    public boolean playRawFile(int rawId) {
        return playRawFile(rawId, null);
    }

    public boolean playRawFile(int rawId, final OnFinishListener onFinishListener) {
        final AssetFileDescriptor fd;
        fd = mContext.getResources().openRawResourceFd(rawId);
        setOnStateChangedListener(new MediaPlayer.OnStateChangedListener() {
            @Override
            public void onStateChanged(MediaPlayer.State from, MediaPlayer.State to) {
                if (to == MediaPlayer.State.End ||
                        to == MediaPlayer.State.Error ||
                        to == MediaPlayer.State.PlaybackCompleted ||
                        to == MediaPlayer.State.Stopped) {
                    setOnStateChangedListener(null);
                    if (onFinishListener != null) {
                        onFinishListener.onFinish();
                    }
                    try {
                        fd.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        });
        boolean ret = play(fd);
        if (!ret) {
            try {
                fd.close();
            } catch (IOException ignored) {
            }
        }
        return ret;
    }

    public void stop() {
        if (mRestrictThread != Thread.currentThread()) {
            throw new RuntimeException("Restrict Thread Exception");
        }
        Log.d(TAG, "stop: then release. " + this);
        releaseLocked();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mMediaPlayer.start();
    }

    public void setOnStateChangedListener(MediaPlayer.OnStateChangedListener listener) {
        if (mRestrictThread != Thread.currentThread()) {
            throw new RuntimeException("Restrict Thread Exception");
        }
        mInnerListener = listener;
    }

    @Override
    public void onStateChanged(MediaPlayer.State from, MediaPlayer.State to) {
        Log.d(TAG, "onStateChanged: from " + from + " to " + to + ", " + this);
        MediaPlayer.OnStateChangedListener listener = mInnerListener;
        if (listener != null) {
            listener.onStateChanged(from, to);
        }
        if (to == MediaPlayer.State.Error ||
                to == MediaPlayer.State.PlaybackCompleted ||
                to == MediaPlayer.State.Stopped) {
            Log.d(TAG, "onStateChanged: then release" + this);
            releaseLocked();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        releaseLocked();
    }

    private void releaseLocked() {
        Log.d(TAG, "releaseLocked: ");
        mMediaPlayer.releaseAsync();
    }

    public interface OnFinishListener {
        void onFinish();
    }
}
