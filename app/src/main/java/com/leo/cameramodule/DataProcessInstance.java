package com.leo.cameramodule;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;

public enum DataProcessInstance implements Runnable {
    INSTANCE;
    private static final String TAGGING = "TAG";

    private Thread mThreadSender;
    private volatile boolean mIsRunning;
    private boolean isPause;
    private byte[] bytes;
    private final ArrayList<byte[]> sendQueue = new ArrayList<>();
    private static final int MAX_SENDQ_SIZE = 30;
    private int dropped = 0;
    private static final int MAX_DROP_PACKET = 10;
    private boolean alive = false;
    private Handler handler;
    private int mWidth, mHeight;

    DataProcessInstance() {
        handler = new Handler(Looper.getMainLooper());
    }

    public boolean ismIsRunning() {
        return mIsRunning;
    }

    public void setmIsRunning(boolean mIsRunning) {
        this.mIsRunning = mIsRunning;
    }

    public boolean start() {
        stopServer();
        mThreadSender = new Thread(this);
        mThreadSender.start();
        mIsRunning = true;
        return true;
    }

    public boolean stopServer() {
        try {
            if (mThreadSender != null && !mThreadSender.isInterrupted()) {
                mThreadSender.interrupt();
            }
            clearQ();
        } catch (Exception e) {
        }

        mIsRunning = false;
        return false;
    }

    private void clearQ() {
        synchronized (sendQueue) {
            sendQueue.clear();
            sendQueue.notify();
            dropped = 0;
        }
    }

    public boolean feedImage(byte[] data, int width, int height) {
        this.mWidth = width;
        this.mHeight = height;
        return sendQ(data);
    }

    boolean sendQ(byte[] data) {
        if (!alive || isPause)
            return false;
        synchronized (sendQueue) {
            if (!canAcceptMore()) {
                dropped++;
                if (dropped < MAX_DROP_PACKET) {
                    Log.d(TAGGING, "Dropped " + dropped + " pkgs");
                } else
                    dropped = 0;
                return false;
            }
            dropped = 0;
            if (sendQueue.size() == 0) {
                sendQueue.add(data);
                sendQueue.notify();
                return true;
            }
            sendQueue.add(data);
            return true;
        }
    }

    private boolean canAcceptMore() {
        synchronized (sendQueue) {
            if (sendQueue.size() == MAX_SENDQ_SIZE)
                return false;
        }
        return true;
    }

    @Override
    public void run() {
        alive = true;
        while (alive) {
            if (isPause) {
                continue;
            }
            byte[] data = null;
            synchronized (sendQueue) {
                if (sendQueue.size() == 0) {
                    try {
                        sendQueue.wait();
                    } catch (Exception ie) {
                        continue;
                    }
                }
                try {
                    data = sendQueue.remove(0);
                } catch (Exception ignored) {
                }
                if (data != null) {
                    try {
                        if (!alive) {
                            break;
                        }
                        byte[] finalData = data;
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (inter != null) {
                                    inter.sendData(finalData);
                                }
                            }
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            mIsRunning = false;
        }
    }

    public boolean isPause() {
        return isPause;
    }

    public void setPause(boolean pause) {
        isPause = pause;
    }

    public IOnSendData inter;

    public IOnSendData getInter() {
        return inter;
    }

    public void setInter(IOnSendData inter) {
        this.inter = inter;
    }
}
