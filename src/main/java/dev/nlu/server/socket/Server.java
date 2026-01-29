package dev.nlu.server.socket;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    static void main() {
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("Server started on port: " + serverSocket.getLocalPort());
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Accepted connection from: " + socket.getInetAddress() + ":" + socket.getPort());
                ServerThread st = new ServerThread(socket);
                st.start();
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
}
