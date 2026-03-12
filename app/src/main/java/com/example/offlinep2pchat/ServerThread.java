package com.example.offlinep2pchat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerThread extends Thread {

    MainActivity activity;

    public ServerThread(MainActivity a) {
        activity = a;
    }

    public void run() {
        try (ServerSocket server = new ServerSocket(8888)) {

            Socket socket = server.accept();
            activity.setSocket(socket);

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    socket.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                activity.showMessage("Client: " + line);
            }

        } catch (Exception ignored) {}
    }
}
