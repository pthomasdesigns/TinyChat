package com.opengarden.testproject;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

public class TinyChatRx {

    private static final String TAG = "TinyChatRx";
    private Handler uiHandler;
    private Thread rxThread = null;
    private Socket socket = null;

    public TinyChatRx(Handler handler) {
        uiHandler = handler;
    }

    void onNetworkAvailable() {
        if (rxThread == null) {
            rxThread = new Thread(new RxThread());
            rxThread.start();
        }
    }

    void onNetworkUnavailable() {
        if (rxThread != null) {
            stop();
            rxThread = null;
        }
    }

    void start() {
        if (TinyChatNetUtils.isNetworkAvailable(TinyChatApplication.getAppContext())) {
            onNetworkAvailable();
        }
    }

    void stop() {
        rxThread.interrupt();
        closeSocket();
        try {
            rxThread.join();
        } catch (Exception e)
        {
            Log.d(TAG, "Join exception" + e);
        }
    }

    class RxThread implements Runnable {

        @Override
        public void run() {

            try {
                socket = new Socket(TinyChatConstants.SERVER_ADDR, TinyChatConstants.SERVER_PORT);
            } catch (Exception e) {
                Log.d(TAG, "Connect exception" + e);
                onRxError("Connect failed");
                return;
            }
            String message;
            BufferedReader input;
            try {
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            } catch (Exception e) {
                Log.d(TAG, "GetInputStream exception" + e);
                onRxError("Connect failed");
                return;
            }

            while (true) {
                if (Thread.interrupted()) {
                    Log.d(TAG, "Interrupted.. exiting");
                    break;
                }
                try {
                    String rxMsg = input.readLine();
                    onRxSuccess(rxMsg);
                } catch (Exception e) {
                    Log.d(TAG, "Read exception" + e);
                    break;
                }
            }

            try {
                socket.close();
            } catch (Exception e) {
                Log.d(TAG, "Close exception" + e);
            }
        }
    }

    private void closeSocket() {
        try {
            socket.close();
        } catch (Exception e) {
            Log.d(TAG, "Close exception" + e);
            return;
        }
    }

    private void onRxError(String error) {
        Message msg = uiHandler.obtainMessage(TinyChatConstants.RX_ERROR, error);
        msg.sendToTarget();
    }

    private void onRxSuccess(String message) {
        Message msg = uiHandler.obtainMessage(TinyChatConstants.RX_MESSAGE, message);
        msg.sendToTarget();
    }
}
