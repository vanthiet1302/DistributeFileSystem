package dev.nlu.server.socket;

import java.io.*;
import java.net.Socket;

public class ServerThread extends Thread {
    private Socket socket;

    public ServerThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (DataInputStream bis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
             DataOutputStream bos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));) {
            bos.writeUTF("Connecting to server: " + socket.getInetAddress().getHostName());
            bos.flush();

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
