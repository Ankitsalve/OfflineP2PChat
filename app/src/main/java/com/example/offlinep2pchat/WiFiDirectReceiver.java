package com.example.offlinep2pchat;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.p2p.*;
import androidx.core.content.ContextCompat;

public class WiFiDirectReceiver extends BroadcastReceiver {

    WifiP2pManager manager;
    WifiP2pManager.Channel channel;
    MainActivity activity;

    public WiFiDirectReceiver(WifiP2pManager m,
                              WifiP2pManager.Channel c,
                              MainActivity a) {
        manager = m;
        channel = c;
        activity = a;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

            if (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED)
                return;

            manager.requestPeers(channel,
                    activity.peerListListener);
        }

        if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

            NetworkInfo info =
                    intent.getParcelableExtra(
                            WifiP2pManager.EXTRA_NETWORK_INFO);

            if (info != null && info.isConnected()) {

                manager.requestConnectionInfo(channel,
                        connection -> {

                            activity.setGroupOwner(
                                    connection.isGroupOwner);

                            activity.setHostAddress(
                                    connection.groupOwnerAddress
                                            .getHostAddress());

                            if (connection.isGroupOwner)
                                activity.startServer();
                            else
                                activity.startClient();
                        });
            }
        }
    }
}
