package com.opengarden.testproject;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class TinyChatMainActivity extends Activity {

    Button b_send;
    EditText et_message;
    TextView tv_response;

    private static Handler uiHandler;
    private static TinyChatRx tinyChatRx;
    private static TinyChatTx tinyChatTx;
    private static TinyChatNetUtils tinyChatNetUtils;

    private static final String TAG = "TinyChatMainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tinychat_activity_main);

        b_send = (Button) findViewById(R.id.b_send);
        et_message = (EditText) findViewById(R.id.et_message);

        tv_response = (TextView) findViewById(R.id.tv_response);
        tv_response.setMovementMethod(new ScrollingMovementMethod());

        createThreads();
        createListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart()");
        tinyChatNetUtils.registerReceiver(this);
        startThreads();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop()");
        tinyChatNetUtils.unregisterReceiver(this);
        stopThreads();
    }

    private void createThreads() {
        uiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                Log.d(TAG, "handleMessage:" + message.what);
                switch(message.what)
                {
                    case TinyChatConstants.RX_ERROR:
                    case TinyChatConstants.RX_MESSAGE:
                    case TinyChatConstants.TX_ERROR:
                        tv_response.append((String) message.obj + "\n");
                        break;
                    case TinyChatConstants.NETWORK_AVAILABLE:
                        tinyChatRx.onNetworkAvailable();
                        tinyChatTx.onNetworkAvailable();
                        break;
                    case TinyChatConstants.NETWORK_NOT_AVAILABLE:
                        tinyChatTx.onNetworkUnavailable();
                        tinyChatRx.onNetworkUnavailable();
                        break;
                    default:
                        break;
                }
            }
        };
        tinyChatRx = new TinyChatRx(uiHandler);
        tinyChatTx = new TinyChatTx(uiHandler);
    }

    private  void startThreads() {
        tinyChatTx.start();
        tinyChatRx.start();
    }

    private  void stopThreads() {
        tinyChatTx.stop();
        tinyChatRx.stop();
    }

    private void createListeners() {
        b_send.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                Log.d("test", "<><> Button Clicked with message: " + et_message.getText().toString());
                sendMessage();
            }
        });

        tinyChatNetUtils = new TinyChatNetUtils(uiHandler);
    }

    private void sendMessage() {
        TinyChatTxMessage txMessage = new TinyChatTxMessage();
        txMessage.msg = et_message.getText().toString();
        txMessage.client_time = Long.toString(System.currentTimeMillis());
        tinyChatTx.sendMessage(txMessage);
    }
}