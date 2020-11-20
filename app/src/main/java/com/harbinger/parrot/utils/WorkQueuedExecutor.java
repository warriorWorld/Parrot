package com.harbinger.parrot.utils;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class WorkQueuedExecutor implements Handler.Callback, WorkWithQuitCheckHandler.OnQuitListener {

    private final String mTag;

    private PreparedHandlerThread mThread;

    private WorkWithQuitCheckHandler mWorkHandler;

    private final long mQuitDelay;

    private final int mPriority;

    private final String mThreadName;

    public static final int NO_QUIT = -1;

    public static final int DEFAULT_DELAY = 3000;

    public interface Callback {
        void handleMessage(Message msg);

        void onQuitSafely();
    }

    private Callback mCallback;

    public WorkQueuedExecutor(String threadName) {
        this(threadName, Thread.NORM_PRIORITY, DEFAULT_DELAY, null);
    }

    public WorkQueuedExecutor(String threadName, Callback callback) {
        this(threadName, Thread.NORM_PRIORITY, DEFAULT_DELAY, callback);
    }

    public WorkQueuedExecutor(String threadName, long quitDelay) {
        this(threadName, Thread.NORM_PRIORITY, quitDelay, null);
    }

    public WorkQueuedExecutor(String threadName, int priority, long quitDelay) {
        this(threadName, priority, quitDelay, null);
    }

    public WorkQueuedExecutor(String threadName, int priority, long quitDelay, Callback callback) {
        mPriority = priority;
        mQuitDelay = quitDelay;
        mThreadName = threadName;
        mTag = "WorkQueuedExecutor." + mThreadName;
        mCallback = callback;
    }

    // make sure thread is synchronized
    private synchronized void checkOrStartThread(PreparedHandlerThread.PrepareCallback callback) {
        if (mThread == null) {
            mThread = new PreparedHandlerThread(mThreadName);
            mThread.setPriority(mPriority);
        }
        if (!mThread.prepare(callback)) {
            mThread = null;
            // try again
            Log.v(mTag, "checkOrStartThread: prepare failed, reset mThread and try again.");
            checkOrStartThread(callback);
        } // else success
    }

    // @Thread(name = "PreparedHandlerThread")
    @Override
    public synchronized boolean onQuit(WorkWithQuitCheckHandler workHandler) {
        Log.w(mTag, "onQuit: workHandler=" + workHandler);
        boolean quitSuccess;
        if (mWorkHandler == workHandler) {
            if (mThread == null) {
                Log.w(mTag, "onQuit: mThread == null, ret");
                quitSuccess = false;
            } else if (!mThread.quitSafely()) {
                Log.w(mTag, "onQuit: cannot quit yet.");
                quitSuccess = false;
            } else {
                Log.v(mTag, "onQuit: quit success");
                mThread = null;
                mWorkHandler = null;
                quitSuccess = true;
            }
        } else {
            Log.w(mTag, "onQuit: WorkHandler not match.", new Throwable());
            quitSuccess = false;
        }
        if (quitSuccess && mCallback != null) {
            mCallback.onQuitSafely();
        }
        return quitSuccess;
    }

    private static class MsgWrapper {
        int what;
        Runnable runnable;
    }

    private final SparseArray<List<MsgWrapper>> mPending = new SparseArray<>();

    public void postDelayed(final Runnable runnable,
                            final long delay) {
        final long uptime = SystemClock.uptimeMillis();
//        Log.v(mTag, "postDelayed: ",
//                " runnable=", runnable,
//                ", delay=", delay);
        final MsgWrapper pend = new MsgWrapper();
        pend.what = 0;
        pend.runnable = runnable;
        synchronized (mPending) {
            List<MsgWrapper> pending = mPending.get(0);
            if (pending == null) {
                pending = new ArrayList<>();
                mPending.put(0, pending);
            }
            pending.add(pend);
        }
        checkOrStartThread(new PreparedHandlerThread.PrepareCallback() {
            // on same thread every time
            // @Thread(name = "PreparedHandlerThread")
            @Override
            public void onPrepared(Looper looper) {
                if (mWorkHandler == null) {
                    mWorkHandler = new WorkWithQuitCheckHandler(
                            mTag,
                            looper,
                            WorkQueuedExecutor.this,
                            WorkQueuedExecutor.this,
                            mQuitDelay);
                }
                synchronized (mPending) {
                    List<MsgWrapper> pending = mPending.get(0);
                    if (pending != null && pending.remove(pend)) {
                        mWorkHandler.postAtTime(runnable, uptime + delay);
                    } else {
                        mWorkHandler.sendEmptyMessage(
                                WorkWithQuitCheckHandler.MSG_WORK_EMPTY_REMOVE);
                    }
                }
            }
        });
    }

    public void sendMessageDelayed(final int what,
                                   final int arg1,
                                   final int arg2,
                                   final Object obj,
                                   final long delay) {
        final long uptime = SystemClock.uptimeMillis();
//        Log.v(mTag, "sendMessageDelayed: ",
//                " what=", what,
//                ", arg1=", arg1,
//                ", arg2=", arg2,
//                ", obj=", obj,
//                ", delay=", delay);
        final MsgWrapper pend = new MsgWrapper();
        pend.what = 0;
        pend.runnable = null;
        synchronized (mPending) {
            List<MsgWrapper> pending = mPending.get(what);
            if (pending == null) {
                pending = new ArrayList<>();
                mPending.put(what, pending);
            }
            pending.add(pend);
        }
        checkOrStartThread(new PreparedHandlerThread.PrepareCallback() {
            // on same thread every time
            // @Thread(name = "PreparedHandlerThread")
            @Override
            public void onPrepared(Looper looper) {
                if (mWorkHandler == null) {
                    mWorkHandler = new WorkWithQuitCheckHandler(
                            mTag,
                            looper,
                            WorkQueuedExecutor.this,
                            WorkQueuedExecutor.this,
                            mQuitDelay);
                }
                synchronized (mPending) {
                    List<MsgWrapper> pending = mPending.get(what);
                    if (pending != null && pending.remove(pend)) {
                        mWorkHandler.sendMessageAtTime(
                                mWorkHandler.obtainMessage(what, arg1, arg2, obj),
                                uptime + delay);
                    } else {
                        mWorkHandler.sendEmptyMessage(
                                WorkWithQuitCheckHandler.MSG_WORK_EMPTY_REMOVE);
                    }
                }
            }
        });
    }

    public void post(final Runnable runnable) {
        postDelayed(runnable, 0);
    }

    public void sendEmptyMessage(final int what) {
        sendMessageDelayed(what, 0, 0, null, 0);
    }

    public void sendEmptyMessageDelayed(final int what, final long delayMillis) {
        sendMessageDelayed(what, 0, 0, null, delayMillis);
    }

    public void sendMessage(final int what,
                            final Object obj) {
        sendMessageDelayed(what, 0, 0, obj, 0);
    }

    public void sendMessage(final int what,
                            final int arg1,
                            final int arg2) {
        sendMessageDelayed(what, arg1, arg2, null, 0);
    }

    public void sendMessage(final int what,
                            final int arg1,
                            final int arg2,
                            final Object obj) {
        sendMessageDelayed(what, arg1, arg2, obj, 0);
    }

    public void removeMessages(int what) {
        synchronized (mPending) {
            List<MsgWrapper> pending = mPending.get(what);
            if (pending != null) {
                pending.clear();
            }
        }
        Handler handler = mWorkHandler;
        if (handler != null) {
            handler.removeMessages(what);
        } else {
            Log.w(mTag,
                    "removeMessages: handler not created yet");
        }
    }

    public void removeCallbacks(Runnable runnable) {
        synchronized (mPending) {
            List<MsgWrapper> pending = mPending.get(0);
            if (pending != null) {
                for (Iterator<MsgWrapper> it = pending.iterator(); it.hasNext(); ) {
                    if (it.next().runnable == runnable) {
                        it.remove();
                    }
                }
            }
        }
        Handler handler = mWorkHandler;
        if (handler != null) {
            handler.removeCallbacks(runnable);
        } else {
            Log.w(mTag,
                    "removeCallbacks: handler not created yet");
        }
    }

    public void removeCallbacksAndMessages() {
        synchronized (mPending) {
            mPending.clear();
        }
        WorkWithQuitCheckHandler handler = mWorkHandler;
        if (handler != null) {
            handler.clearMessages();
        } else {
            Log.w(mTag,
                    "removeCallbacksAndMessages: handler not created yet");
        }
    }

    @Deprecated
    public void removeCallbacksAndMessages(Object obj) {
        removeCallbacksAndMessages();
    }

    @Override
    public final boolean handleMessage(Message msg) {
        if (mCallback != null) {
            mCallback.handleMessage(msg);
        }
        return true;
    }

    public interface RunnableForWait<T> {
        T run();
    }

    private static class RunnableForWaitResult<T> {
        boolean isReturn;
        T returnMsg;
    }

    public static class RunnableForWaitTimeoutException extends Exception {
        RunnableForWaitTimeoutException(String msg) {
            super(msg);
        }
    }

    public <T> T postAndWait(final RunnableForWait<T> runnable) throws RunnableForWaitTimeoutException {
        if (runnable == null) {
            throw new RuntimeException("runnable cannot be null in postAndWait");
        }
        final RunnableForWaitResult<T> result = new RunnableForWaitResult<T>();
        synchronized (result) {
            post(new Runnable() {
                @Override
                public void run() {
                    result.returnMsg = runnable.run();
                    result.isReturn = true;
                    synchronized (result) {
                        result.notify();
                    }
                }
            });
            try {
                result.wait(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (result.isReturn) {
            return result.returnMsg;
        } else {
            throw new RunnableForWaitTimeoutException("!!! postAndWait (5s) RUN TIMEOUT !!!");
        }
    }
}

final class WorkWithQuitCheckHandler extends Handler {

    private final String mTag;

    private static final int MSG_WORK_QUIT_LOOPER = (int) (System.currentTimeMillis() / 1000);
    // 由于在post一个消息后有可能被立刻remove, 此时无法真正往Looper中加入一个msg, 则无法触发delayquit
    // MSG_WORK_EMPTY_REMOVE就是在此情况下模拟发送一个不执行的msg
    static final int MSG_WORK_EMPTY_REMOVE = MSG_WORK_QUIT_LOOPER + 1;

    interface OnQuitListener {
        boolean onQuit(WorkWithQuitCheckHandler workHandler);
    }

    private final long mQuitDelay;

    private final OnQuitListener mQuitListener;

    // add only, won't remove
    private final Set<Integer> mHistoryWhats = new HashSet<>();

    void clearMessages() {
        synchronized (mHistoryWhats) {
            for (Integer what : mHistoryWhats) {
                removeMessages(what);
            }
        }
    }

    WorkWithQuitCheckHandler(String owner,
                             Looper looper,
                             Callback callback,
                             OnQuitListener quitListener,
                             long quitDelay) {
        super(looper, callback);
        mTag = owner + "." + "WorkWithQuitCheckHandler";
        mQuitListener = quitListener;
        mQuitDelay = quitDelay;
    }

    @Override
    public boolean sendMessageAtTime(Message msg, long uptimeMillis) {
        synchronized (mHistoryWhats) {
            if (msg.what != MSG_WORK_QUIT_LOOPER && msg.what != MSG_WORK_EMPTY_REMOVE) {
                mHistoryWhats.add(msg.what);
            }
        }
        return super.sendMessageAtTime(msg, uptimeMillis);
    }

    // @Thread(name = "PreparedHandlerThread")
    @Override
    public void dispatchMessage(Message msg) {
        // MSG_WORK_QUIT_LOOPER, quit or not
        synchronized (mHistoryWhats) {
            boolean quit = msg.what == MSG_WORK_QUIT_LOOPER
                    && !hasAnyMessage(mHistoryWhats)
                    && mQuitListener.onQuit(this);
            if (quit) {
                mHistoryWhats.clear();
                return;
            }
        }
        // ignore MSG_WORK_QUIT_LOOPER, NO NEED handled
        if (msg.what != MSG_WORK_QUIT_LOOPER && msg.what != MSG_WORK_EMPTY_REMOVE) {
            super.dispatchMessage(msg);
        }
        // delay MSG_WORK_QUIT_LOOPER again
        if (mQuitDelay > 0) {
            removeMessages(MSG_WORK_QUIT_LOOPER);
            sendEmptyMessageDelayed(MSG_WORK_QUIT_LOOPER, mQuitDelay);
        }
    }

    private boolean hasAnyMessage(Collection<Integer> histories) {
        boolean has = false;
        for (Integer what : histories) {
            if (hasMessages(what)) {
                has = true;
                break;
            }
        }
        if (!has) {
            int runnableWhat = 0;
            has = hasMessages(runnableWhat);
        }
        Log.v(mTag, "hasAnyMessage: check to QUIT, CHECK RESULT is " + has);
        return has;
    }
}
