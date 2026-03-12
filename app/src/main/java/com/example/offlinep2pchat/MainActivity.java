package com.example.offlinep2pchat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.*;
import android.os.Build;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Button btnDiscover, btnSend;
    ListView listView;
    TextView txtChat, txtStatus;
    EditText editMessage;

    WifiP2pManager manager;
    WifiP2pManager.Channel channel;
    WiFiDirectReceiver receiver;
    IntentFilter intentFilter;

    List<WifiP2pDevice> peers = new ArrayList<>();
    ArrayAdapter<String> adapter;

    boolean isGroupOwner = false;
    String hostAddress;
    Socket socket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnDiscover = findViewById(R.id.btnDiscover);
        btnSend = findViewById(R.id.btnSend);
        listView = findViewById(R.id.listView);
        txtChat = findViewById(R.id.txtChat);
        txtStatus = findViewById(R.id.txtStatus);
        editMessage = findViewById(R.id.editMessage);

        manager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);

        receiver = new WiFiDirectReceiver(manager, channel, this);

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                new ArrayList<>());
        listView.setAdapter(adapter);

        requestPermissions();

        btnDiscover.setOnClickListener(v -> discoverPeers());

        listView.setOnItemClickListener((p, v, pos, id) ->
                connectToDevice(peers.get(pos)));

        btnSend.setOnClickListener(v -> sendMessage());
    }

    // ---------- PERMISSION ----------
    private boolean hasPermissions() {
        boolean loc = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (Build.VERSION.SDK_INT >= 33) {
            boolean near = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.NEARBY_WIFI_DEVICES)
                    == PackageManager.PERMISSION_GRANTED;
            return loc && near;
        }
        return loc;
    }

    private void requestPermissions() {
        List<String> list = new ArrayList<>();
        list.add(Manifest.permission.ACCESS_FINE_LOCATION);

        if (Build.VERSION.SDK_INT >= 33)
            list.add(Manifest.permission.NEARBY_WIFI_DEVICES);

        ActivityCompat.requestPermissions(this,
                list.toArray(new String[0]), 1);
    }

    // ---------- DISCOVER ----------
    @SuppressLint("MissingPermission")
    private void discoverPeers() {
        if (!hasPermissions()) return;

        manager.discoverPeers(channel,
                new WifiP2pManager.ActionListener() {
                    public void onSuccess() {
                        txtStatus.setText("Discovering...");
                    }
                    public void onFailure(int reason) {
                        txtStatus.setText("Discover failed");
                    }
                });
    }

    // ---------- CONNECT ----------
    @SuppressLint("MissingPermission")
    private void connectToDevice(WifiP2pDevice device) {
        if (!hasPermissions()) return;

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;

        manager.connect(channel, config,
                new WifiP2pManager.ActionListener() {
                    public void onSuccess() {
                        txtStatus.setText("Connecting...");
                    }
                    public void onFailure(int reason) {
                        txtStatus.setText("Connect failed");
                    }
                });
    }

    // ---------- CHAT ----------
    private void sendMessage() {
        try {
            if (socket == null) return;

            String msg = editMessage.getText().toString();
            if (msg.isEmpty()) return;

            new Thread(() -> {
                try {
                    socket.getOutputStream()
                            .write((msg + "\n").getBytes());
                    runOnUiThread(() -> {
                        txtChat.append("Me: " + msg + "\n");
                        editMessage.setText("");
                    });
                } catch (Exception ignored) {}
            }).start();

        } catch (Exception ignored) {}
    }

    public void showMessage(String msg) {
        runOnUiThread(() ->
                txtChat.append(msg + "\n"));
    }

    public void setSocket(Socket s) {
        this.socket = s;
    }

    // ---------- SERVER/CLIENT ----------
    public void startServer() {
        txtStatus.setText("Connected (Server)");
        new ServerThread(this).start();
    }

    public void startClient() {
        txtStatus.setText("Connected (Client)");
        new ClientThread(this, hostAddress).start();
    }

    public void setGroupOwner(boolean owner) {
        isGroupOwner = owner;
    }

    public void setHostAddress(String host) {
        hostAddress = host;
    }

    // ---------- PEER LIST ----------
    public WifiP2pManager.PeerListListener peerListListener =
            peerList -> {
                peers.clear();
                peers.addAll(peerList.getDeviceList());

                List<String> names = new ArrayList<>();
                for (WifiP2pDevice d : peers)
                    names.add(d.deviceName);

                adapter.clear();
                adapter.addAll(names);
            };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }
}
