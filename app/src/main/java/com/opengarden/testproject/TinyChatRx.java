package com.opengarden.testproject;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

public class TinyChatRx {

    private static final String TAG = "TinyChatRx";
    private Handler uiHandler;
    private static HandlerThread rxThread;
    private static Handler rxHandler;
    private Socket socket = null;
    private int numRetries = 0;

    public TinyChatRx(Handler handler) {
        uiHandler = handler;
    }

    void onNetworkAvailable() {
        createSocket(0);
    }

    void onNetworkUnavailable() {
        closeSocket();
    }

    void start() {
        rxThread = new HandlerThread("Rx-Thread");
        rxThread.start();
        rxHandler = new Handler(rxThread.getLooper()) {
            @Override
            public void handleMessage(Message message) {
                Log.d(TAG, "handleMessage:" + message.what);
                switch(message.what)
                {
                case TinyChatConstants.SOCKET_ERROR:
                    if (numRetries++ < TinyChatConstants.MAX_RETRIES) {
                        createSocket(1000);
                    } else {
                        Log.d(TAG, "Max retries exceeded");
                    }
                    break;
                case TinyChatConstants.SOCKET_SUCCESS:
                    numRetries = 0;
                    receiveMessage();
                    break;
                default:
                    break;
                }
            }
        };
        if (TinyChatNetUtils.isNetworkAvailable(TinyChatApplication.getAppContext())) {
            onNetworkAvailable();
        }
    }

    void stop() {
        closeSocket();
        rxThread.quit();
    }

    private void createSocket(int delay)
    {
        if (socket != null) return;
        rxHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket(TinyChatConstants.SERVER_ADDR, TinyChatConstants.SERVER_PORT);
                } catch (Exception e) {
                    Log.d(TAG, "Connect exception" + e);
                    onSocketFailed();
                    return;
                }
                onSocketSuccess();
            }
        }, delay);
    }

    private void closeSocket()
    {
        try {
            if (socket != null) socket.close();
            socket = null;
        } catch (Exception e) {
            Log.d(TAG, "Close exception" + e);
            return;
        }
    }


    public void receiveMessage() {
        rxHandler.post(new Runnable() {
            @Override
            public void run() {

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
                closeSocket();
            }
        });
    }

    private void onRxError(String error) {
        Message msg = uiHandler.obtainMessage(TinyChatConstants.RX_ERROR, error);
        msg.sendToTarget();
    }

    private void onRxSuccess(String message) {
        Message msg = uiHandler.obtainMessage(TinyChatConstants.RX_MESSAGE, message);
        msg.sendToTarget();
    }

    private void onSocketFailed() {
        Message msg = rxHandler.obtainMessage(TinyChatConstants.SOCKET_ERROR, "Socket Error");
        msg.sendToTarget();
    }

    private void onSocketSuccess() {
        Message msg = rxHandler.obtainMessage(TinyChatConstants.SOCKET_SUCCESS, "Socket Success");
        msg.sendToTarget();
    }
}
