package com.harbinger.parrot.utils;

import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Log;


import java.util.ArrayList;
import java.util.List;

public class PreparedHandlerThread extends Thread {

    private static final String TAG = "HandlerThread";

    private static final int INVALID_TID = -1;

    private int mPriority;

    private int mTid = INVALID_TID;

    private final Once<Looper> mLooper = new Once<>();

    private Handler mHandler;

    private Handler mInnerH;

    private final Object mPrepareLock = new Object();

    private final Object mStartLock = new Object();

    private boolean mStarted;

    private static class Once<T> {

        private T t;

        /**
         * NEVER get another value after get the first NOTNULL.
         */
        public synchronized T get() {
            return t;
        }

        /**
         * NEVER change after value set
         */
        public synchronized void set(T t) {
            if (this.t != null) {
                throw new RuntimeException("can NOT set twice.");
            }
            this.t = t;
        }
    }

    // set null after thread started
    private List<PrepareCallback> mPrepareCallbacks = new ArrayList<>();

    public PreparedHandlerThread(String name) {
        super(name);
        mPriority = Process.THREAD_PRIORITY_DEFAULT;
    }

    public interface PrepareCallback {
        /**
         * invoked on Work Thread, and no sync LOCK on it.
         */
        void onPrepared(Looper looper);
    }

    /**
     * return false if thread has been INVALID.
     */
    public boolean prepare(final PrepareCallback callback) {
        synchronized (mStartLock) {
            if (!mStarted) {
                mStarted = true;
                start();
            }
        }
        synchronized (mPrepareLock) {
            final Looper looper = mLooper.get();
            if (looper != null) {
                if (!mQuit) {
                    // we make sure thread available when preparing
                    mPrepareCallbacks.add(callback);
                    // init inner handle
                    Handler handler = mInnerH;
                    if (handler == null) {
                        handler = new Handler(looper);
                        mInnerH = handler;
                    }
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            onLooperPreparedInner();
                        }
                    });
                } else {
                    Log.e(TAG, "prepare: thread maybe stopped.", new Throwable());
                    return false;
                }
            } else {
                mPrepareCallbacks.add(callback);
            }
            return true;
        }
    }

    /**
     * see {@link android.os.HandlerThread#HandlerThread(String, int)}
     */
    public PreparedHandlerThread(String name, int priority) {
        super(name);
        mPriority = priority;
    }

    /**
     * see {@link android.os.HandlerThread#onLooperPrepared()}
     */
    protected void onLooperPrepared() {
    }

    private void onLooperPreparedInner() {
        List<PrepareCallback> prepareCallbacks;
        synchronized (mPrepareLock) {
            prepareCallbacks = new ArrayList<>(mPrepareCallbacks);
            mPrepareCallbacks.clear();
        }
        for (PrepareCallback callback : prepareCallbacks) {
            callback.onPrepared(mLooper.get());
        }
    }

    @Override
    public void run() {
        mTid = Process.myTid();
        Looper.prepare();
        // synchronized for notify getLooper(). Deprecated.
        synchronized (this) {
            // set Looper here ONLY.
            mLooper.set(Looper.myLooper());
            notifyAll();
        }
        Process.setThreadPriority(mPriority);
        onLooperPrepared();
        onLooperPreparedInner();
        Looper.loop();
        mTid = INVALID_TID;
    }

    /**
     * see {@link android.os.HandlerThread#getLooper()} <br/>
     * Actually, we recommend to get a NULL when not prepared.
     */
    @Deprecated
    public Looper getLooper() {
        if (!isAlive()) {
            return null;
        }

        // If the thread has been started, wait until the looper has been created.
        synchronized (this) {
            while (isAlive() && mLooper.get() == null) {
                try {
                    wait();
                } catch (InterruptedException ignore) {
                }
            }
        }
        return mLooper.get();
    }

    /**
     * @return a shared {@link Handler} associated with this thread
     */
    @Deprecated
    public Handler getThreadHandler() {
        if (mHandler == null) {
            mHandler = new Handler(getLooper());
        }
        return mHandler;
    }

    /**
     * see {@link android.os.HandlerThread#quit()}
     * we don't recommend it
     */
    @Deprecated
    private boolean quit() {
        Looper looper = getLooper();
        if (looper != null) {
            looper.quit();
            return true;
        }
        return false;
    }

    private boolean mQuit;

    /**
     * see {@link android.os.HandlerThread#quitSafely()}
     */
    public boolean quitSafely() {
        synchronized (mPrepareLock) {
            if (mPrepareCallbacks.size() > 0) {
                Log.w(TAG, "quitSafely: preparing, quit failed.");
                return false;
            } else {
                mQuit = true;
            }
        }
        Looper looper = mLooper.get();
        if (looper != null) {
            Log.w(TAG, "quitSafely: let's quit.");
            looper.quitSafely();
        } else {
            throw new RuntimeException("quitSafely: Looper is null???");
        }
        return true;
    }

    /**
     * see {@link android.os.HandlerThread#getThreadId()}
     */
    @Deprecated
    public int getThreadId() {
        return mTid;
    }
}
