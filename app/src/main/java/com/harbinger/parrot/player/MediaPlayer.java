package com.harbinger.parrot.player;

import android.content.Context;
import android.net.Uri;
import android.util.Log;


import com.harbinger.parrot.utils.WorkQueuedExecutor;

import java.io.FileDescriptor;
import java.io.IOException;

/**
 * <p>
 * A common wrapper edition of MediaPlayer.
 * We complete it with all useful states.
 * All api has a return code.
 */
public class MediaPlayer
        implements
        android.media.MediaPlayer.OnPreparedListener,
        android.media.MediaPlayer.OnErrorListener,
        android.media.MediaPlayer.OnSeekCompleteListener,
        android.media.MediaPlayer.OnCompletionListener {

    /**
     * A return of apis. Success
     */
    public static final int RET_SUCCESS = 0;
    public static final int RET_SUCCESS_INVALID = 1;
    public static final int RET_ERROR_NO_CACHE_SPACE = -50;
    /**
     * A return of apis. IO Exception
     */
    public static final int RET_ERROR_IO = -101;
    /**
     * A return of apis. IllegalStateException
     */
    public static final int RET_ERROR_ILLEGAL_STATE = -102;
    /**
     * A return of apis. IllegalArgumentException
     */
    public static final int RET_ERROR_ILLEGAL_ARGUMENT = -103;
    public static final int MEDIA_ERROR_UNKNOWN = android.media.MediaPlayer.MEDIA_ERROR_UNKNOWN;
    public static final int MEDIA_ERROR_SERVER_DIED =
            android.media.MediaPlayer.MEDIA_ERROR_SERVER_DIED;
    public static final int MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK =
            android.media.MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK;
    public static final int MEDIA_ERROR_IO = android.media.MediaPlayer.MEDIA_ERROR_IO;
    public static final int MEDIA_ERROR_MALFORMED = android.media.MediaPlayer.MEDIA_ERROR_MALFORMED;
    public static final int MEDIA_ERROR_UNSUPPORTED =
            android.media.MediaPlayer.MEDIA_ERROR_UNSUPPORTED;
    public static final int MEDIA_ERROR_TIMED_OUT = android.media.MediaPlayer.MEDIA_ERROR_TIMED_OUT;
    private static final String TAG = "MediaPlayer";
    private android.media.MediaPlayer mImpl;
    /**
     * State of media player.
     * The details of state diagram, to see the link:
     * (https://developer.android.com/reference/android/media/MediaPlayer.html)
     */
    private State mState = State.Idle;
    private boolean mIsSeeking;
    private OnStateChangedListener mOnStateChangedListener;
    private OnPreparedListener mOnPreparedListener;
    private OnErrorListener mOnErrorListener;
    private OnSeekCompleteListener mOnSeekCompleteListener;
    private OnCompletionListener mOnCompletionListener;


    public MediaPlayer() {
        createAndroidMediaPlayer();
    }

    private void createAndroidMediaPlayer() {
        mImpl = new android.media.MediaPlayer();
        mImpl.setOnPreparedListener(this);
        mImpl.setOnErrorListener(this);
        mImpl.setOnCompletionListener(this);
        mImpl.setOnSeekCompleteListener(this);
    }

    private void reCreateAndroidMediaPlayer() {
        Log.d(TAG, "reCreateAndroidMediaPlayer() called");
        if (mImpl != null) {
            try {
                mImpl.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                mImpl.reset();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                mImpl.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mImpl = null;
        }

        createAndroidMediaPlayer();
        changeStateLocked(State.Idle);
    }

    public boolean isSeeking() {
        return mIsSeeking;
    }

    public void setOnStateChangedListener(OnStateChangedListener listener) {
        mOnStateChangedListener = listener;
    }

    public State getCurrentState() {
        return mState;
    }

    private static final int INVALID_POSITION = -1;

    public int getCurrentPosition() {
        if (checkStateLocked(State.Started, State.Paused)) {
            try {
                return mImpl.getCurrentPosition();
            } catch (IllegalStateException e) {
                e.printStackTrace();
                reCreateAndroidMediaPlayer();
            }
        }
        return INVALID_POSITION;
    }

    private static final int INVALID_DURATION = -1;

    public int getDuration() {
        if (checkStateLocked(State.Started, State.Paused)) {
            try {
                return mImpl.getDuration();
            } catch (IllegalStateException e) {
                e.printStackTrace();
                reCreateAndroidMediaPlayer();
            }
        }
        return INVALID_DURATION;
    }


    public void setLooping(boolean looping) {
        try {
            mImpl.setLooping(looping);
        } catch (IllegalStateException e) {
            e.printStackTrace();
            reCreateAndroidMediaPlayer();
        }
    }

    // maybe called by several threads.
    private void changeStateLocked(State state) {
        if (state == null) {
            throw new IllegalArgumentException(
                    "Error when call changeState(State state), state cannot be null!");
        }
        State old = mState;
        mState = state;
        Log.d(TAG, "[changeStateLocked]: state change from " + old + " to " + state);
        if (mOnStateChangedListener != null && old != state) {
            mOnStateChangedListener.onStateChanged(old, state);
        }
    }

    /**
     * we don't support prepare sync method. equals prepareAsync.
     */
    public int prepare() {
        Log.d(TAG, "[prepare]: current state is " + mState);
        return prepareAsync();
    }

    /**
     * success only when State.Stopped or State.Initialized.
     */
    public int prepareAsync() {
        Log.d(TAG, "[prepareAsync]: current state is " + mState);
        if (!checkStateLocked(State.Stopped, State.Initialized)) {
            return RET_ERROR_ILLEGAL_STATE;
        }
        try {
            mImpl.prepareAsync();
            changeStateLocked(State.Preparing);
            return RET_SUCCESS;
        } catch (IllegalStateException e) {
            e.printStackTrace();
            reCreateAndroidMediaPlayer();
            return RET_ERROR_ILLEGAL_STATE;
        }
    }

    public void setOnPreparedListener(OnPreparedListener listener) {
        mOnPreparedListener = listener;
    }

    @Override
    public void onPrepared(android.media.MediaPlayer mp) {
        Log.d(TAG, "[onPrepared]");
        if (mImpl != null && mp == mImpl) {
            changeStateLocked(State.Prepared);
            if (mOnPreparedListener != null) {
                mOnPreparedListener.onPrepared(this);
            }
        }
    }

    /**
     * success only when
     *
     * @return
     */
    public int start() {
        Log.d(TAG, "[start]: current state is " + mState);
        int res;
        if (!checkStateLocked(State.Started, State.Paused, State.Prepared, State.PlaybackCompleted)) {
            res = RET_ERROR_ILLEGAL_STATE;
        } else if (checkStateLocked(State.Started)) {
            res = RET_SUCCESS_INVALID;
        } else {
            try {
                mImpl.start();
                changeStateLocked(State.Started);
                res = RET_SUCCESS;
            } catch (IllegalStateException e) {
                e.printStackTrace();
                reCreateAndroidMediaPlayer();
                res = RET_ERROR_ILLEGAL_STATE;
            }
        }
        return res;

    }

    public int pause() {
        Log.d(TAG, "[pause]: current state is " + mState);
        int res;
        if (!checkStateLocked(State.Started, State.Paused)) {
            res = RET_ERROR_ILLEGAL_STATE;
        } else if (checkStateLocked(State.Paused)) {
            res = RET_SUCCESS_INVALID;
        } else {
            try {
                mImpl.pause();
                changeStateLocked(State.Paused);
                res = RET_SUCCESS;
            } catch (IllegalStateException e) {
                e.printStackTrace();
                reCreateAndroidMediaPlayer();
                res = RET_ERROR_ILLEGAL_STATE;
            }
        }
        return res;

    }

    public int stop() {
        Log.d(TAG, "[stop]: current state is " + mState);
        int res;
        if (!checkStateLocked(State.Stopped, State.Started, State.Paused, State.Prepared,
                State.PlaybackCompleted)) {
            res = RET_ERROR_ILLEGAL_STATE;
        } else if (checkStateLocked(State.Stopped)) {
            res = RET_SUCCESS_INVALID;
        } else {
            try {
                mImpl.stop();
                changeStateLocked(State.Stopped);
                res = RET_SUCCESS;
            } catch (IllegalStateException e) {
                e.printStackTrace();
                reCreateAndroidMediaPlayer();
                res = RET_ERROR_ILLEGAL_STATE;
            }
        }
        return res;

    }

    public int setDataSource(Context context, Uri uri) {
        Log.d(TAG, "[setDataSource]: current state is " + mState);
        int res;
        if (!checkStateLocked(State.Idle)) {
            res = RET_ERROR_ILLEGAL_STATE;
        } else {
            try {
                mImpl.setDataSource(context, uri);
                changeStateLocked(State.Initialized);
                res = RET_SUCCESS;
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                res = RET_ERROR_ILLEGAL_ARGUMENT;
            } catch (IllegalStateException e) {
                e.printStackTrace();
                reCreateAndroidMediaPlayer();
                res = RET_ERROR_ILLEGAL_STATE;
            } catch (IOException e) {
                e.printStackTrace();
                res = RET_ERROR_IO;
            }
        }
        return res;

    }

    public int setDataSource(Context context, FileDescriptor fd, long startOffset, long length) {
        Log.d(TAG, "[setDataSource]: current state is " + mState);
        int res;
        if (!checkStateLocked(State.Idle)) {
            res = RET_ERROR_ILLEGAL_STATE;
        } else {
            try {
                mImpl.setDataSource(fd, startOffset, length);
                changeStateLocked(State.Initialized);
                res = RET_SUCCESS;
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                res = RET_ERROR_ILLEGAL_ARGUMENT;
            } catch (IllegalStateException e) {
                e.printStackTrace();
                reCreateAndroidMediaPlayer();
                res = RET_ERROR_ILLEGAL_STATE;
            } catch (IOException e) {
                e.printStackTrace();
                res = RET_ERROR_IO;
            }
        }
        return res;

    }

    public int reset() {
        Log.d(TAG, "[reset]: current state is " + mState);
        try {
            mImpl.reset();
        } catch (IllegalStateException | IllegalArgumentException e) {
            e.printStackTrace();
            reCreateAndroidMediaPlayer();
        }
        changeStateLocked(State.Idle);
        return RET_SUCCESS;
    }

    public void setOnErrorListener(OnErrorListener listener) {
        mOnErrorListener = listener;
    }

    @Override
    public boolean onError(android.media.MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "[onError]: what=" + what + ", extra=" + extra);
        changeStateLocked(State.Error);
        if (mImpl != null && mp == mImpl && mOnErrorListener != null) {
            return mOnErrorListener.onError(this, what, extra);
        } else {
            return false;
        }
    }

    public void setOnSeekCompleteListener(OnSeekCompleteListener listener) {
        mOnSeekCompleteListener = listener;
    }

    @Override
    public void onSeekComplete(android.media.MediaPlayer mp) {
        Log.d(TAG, "[onSeekComplete]");
        mIsSeeking = false;
        if (mImpl != null && mp == mImpl && mOnSeekCompleteListener != null) {
            mOnSeekCompleteListener.onSeekComplete(this);
        }
    }

    public void setOnCompletionListener(OnCompletionListener listener) {
        mOnCompletionListener = listener;
    }

    @Override
    public void onCompletion(android.media.MediaPlayer mp) {
        Log.d(TAG, "[onCompletion]");
        changeStateLocked(State.PlaybackCompleted);
        if (mImpl != null && mp == mImpl && mOnCompletionListener != null) {
            mOnCompletionListener.onCompletion(this);
        }
    }

    private static final int INVALID_SESSION_ID = -1;

    public int getAudioSessionId() {

        try {
            return mImpl.getAudioSessionId();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            reCreateAndroidMediaPlayer();
        }
        return INVALID_SESSION_ID;
    }

    public int setVolume(float leftVolume, float rightVolume) {
        try {
            mImpl.setVolume(leftVolume, rightVolume);
        } catch (IllegalStateException e) {
            e.printStackTrace();
            reCreateAndroidMediaPlayer();
        }
        return RET_SUCCESS;
    }

    public int seekTo(int msec) {
        Log.d(TAG, "[seekTo]: current state is " + mState);
        if (!checkStateLocked(State.Prepared, State.Started, State.Paused, State.PlaybackCompleted)) {
            return RET_ERROR_ILLEGAL_STATE;
        }
        try {
            mImpl.seekTo(msec);
            mIsSeeking = true;
            return RET_SUCCESS;
        } catch (IllegalStateException e) {
            e.printStackTrace();
            reCreateAndroidMediaPlayer();
            return RET_ERROR_ILLEGAL_STATE;
        }
    }

    public void release() {
        Log.e(TAG, "[release]"); // E level log
        try {
            mImpl.release();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
        changeStateLocked(State.End);
    }

    // TODO: 2019/5/18 Rom中MediaPlayer有弱网卡住的bug, 这时连release方法都是有可能卡住的. 在这里我们放在异步线程做
    private WorkQueuedExecutor mReleaseExecutorForMediaPlayer = new WorkQueuedExecutor(TAG + ".Release");

    public void releaseAsync() {
        Log.e(TAG, "[releaseAsync]"); // E level log
        mReleaseExecutorForMediaPlayer.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mImpl.reset();
                    mImpl.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        changeStateLocked(State.End);
    }

    private boolean checkStateLocked(State... states) {
        for (State state : states) {
            if (mState == state) {
                Log.d(TAG, "[checkStateLocked]: OK");
                return true;
            }
        }
        Log.e(TAG, "[checkStateLocked]: FAILED");
        return false;
    }

    /**
     * inner state of MediaPlayer
     */
    public enum State {
        Idle,
        Initialized,
        Preparing,
        Prepared,
        Started,
        Stopped,
        Paused,
        PlaybackCompleted,
        Error,
        End,
    }

    public interface OnStateChangedListener {
        void onStateChanged(State from, State to);
    }

    public interface OnPreparedListener {
        void onPrepared(MediaPlayer mp);
    }

    public interface OnErrorListener {
        boolean onError(MediaPlayer mp, int what, int extra);
    }

    public interface OnSeekCompleteListener {
        void onSeekComplete(MediaPlayer mp);
    }

    public interface OnCompletionListener {
        void onCompletion(MediaPlayer mp);
    }
}
