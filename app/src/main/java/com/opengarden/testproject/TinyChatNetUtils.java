package com.opengarden.testproject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

public class TinyChatNetUtils {
    private static final String TAG = "TinyChatNetUtils";
    private static NetworkStateReceiver receiver = null;
    private Handler uiHandler;

    TinyChatNetUtils(Handler handler) {
        uiHandler = handler;
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        return isConnected;
    }

    public class NetworkStateReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Network connectivity change");
            if (intent.getExtras() != null) {
                NetworkInfo ni = (NetworkInfo) intent.getExtras().get(ConnectivityManager.EXTRA_NETWORK_INFO);
                if (ni != null && ni.getState() == NetworkInfo.State.CONNECTED) {
                    Log.d(TAG, "Network " + ni.getTypeName() + " connected");
                    Message msg = uiHandler.obtainMessage(TinyChatConstants.NETWORK_AVAILABLE, "Network Available");
                    msg.sendToTarget();
                } else if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, Boolean.FALSE)) {
                    Log.d(TAG, "No network connectivity");
                    Message msg = uiHandler.obtainMessage(TinyChatConstants.NETWORK_NOT_AVAILABLE, "Network Not Available");
                    msg.sendToTarget();
                }
            }
        }
    }

    public void registerReceiver(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new NetworkStateReceiver();
        context.registerReceiver(receiver,filter);
    }

    public void unregisterReceiver(Context context) {
        context.unregisterReceiver(receiver);
    }
}
