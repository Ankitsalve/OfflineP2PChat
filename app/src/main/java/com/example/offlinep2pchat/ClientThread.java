package com.example.offlinep2pchat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

public class ClientThread extends Thread {

    MainActivity activity;
    String host;

    public ClientThread(MainActivity a, String h) {
        activity = a;
        host = h;
    }

    public void run() {
        try {
            Socket socket = new Socket(host, 8888);
            activity.setSocket(socket);

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    socket.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                activity.showMessage("Server: " + line);
            }

        } catch (Exception ignored) {}
    }
}
