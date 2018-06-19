package com.opengarden.testproject;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import com.google.gson.Gson;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class TinyChatTx {
    private static final String TAG = "TinyChatRx";
    private static HandlerThread txThread;
    private static Handler txHandler;
    private Handler uiHandler;
    private static Gson gson;
    Socket socket;
    private int numRetries = 0;

    public TinyChatTx(Handler handler) {
        uiHandler = handler;
        gson = new Gson();
    }

    public void start() {
        txThread = new HandlerThread("Tx-Thread");
        txThread.start();
        txHandler = new Handler(txThread.getLooper()) {
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
                        sendPendingMessages();
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

    public void onNetworkAvailable() {
        createSocket(0);
   }

    public void onNetworkUnavailable() {
        closeSocket();
    }

    public void stop() {
        closeSocket();
        txThread.quit();
    }

    private void createSocket(int delay)
    {
        if (socket != null) return;
        txHandler.postDelayed(new Runnable() {
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

    private void sendPendingMessages() {
        txHandler.post(new Runnable() {
            @Override
            public void run() {
                List<TinyChatTxMessage> messages = TinyChatTxMessageDatabase.getInstance(TinyChatApplication.
                        getAppContext()).tinyChatTxMessageDao().getAll();
                try {
                    PrintWriter out = new PrintWriter(new BufferedWriter(
                            new OutputStreamWriter(socket.getOutputStream())),
                            true);
                    for (TinyChatTxMessage message : messages) {
                        String msg = gson.toJson(message);
                        Log.d(TAG, "Message: " + msg);
                        out.println(msg);
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Write exception" + e);
                    onSendFailed();
                }
                TinyChatTxMessageDatabase.getInstance(TinyChatApplication.
                        getAppContext()).tinyChatTxMessageDao().deleteAll();
                Log.d(TAG, "Send success");
            }
        });
    }

    public void sendMessage(final TinyChatTxMessage message) {

        txHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!TinyChatNetUtils.isNetworkAvailable(TinyChatApplication.getAppContext())) {
                    TinyChatTxMessageDatabase.getInstance(TinyChatApplication.getAppContext())
                            .tinyChatTxMessageDao().insert(message);
                    onMessageSaved();
                    return;
                }
                if (socket == null) {
                    onSendFailed();
                    return;
                }
                try {
                    PrintWriter out = new PrintWriter(new BufferedWriter(
                            new OutputStreamWriter(socket.getOutputStream())),
                            true);
                    String msg = gson.toJson(message);
                    Log.d(TAG, "Message: " + msg);
                    out.println(msg);
                } catch (Exception e) {
                    Log.d(TAG, "Write exception" + e);
                    onSendFailed();
                }
                Log.d(TAG, "Send success");
            }
        });
    }

    private void onSendFailed() {
        Message msg = uiHandler.obtainMessage(TinyChatConstants.TX_ERROR, "Send failed");
        msg.sendToTarget();
    }

    private void onMessageSaved() {
        Message msg = uiHandler.obtainMessage(TinyChatConstants.TX_ERROR, "Message saved");
        msg.sendToTarget();
    }

    private void onSocketFailed() {
        Message msg = txHandler.obtainMessage(TinyChatConstants.SOCKET_ERROR, "Socket Error");
        msg.sendToTarget();
    }

    private void onSocketSuccess() {
        Message msg = txHandler.obtainMessage(TinyChatConstants.SOCKET_SUCCESS, "Socket Success");
        msg.sendToTarget();
    }
}
