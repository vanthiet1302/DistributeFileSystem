package dev.nlu.server.socket;

import dev.nlu.model.User;
import dev.nlu.utils.DBUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;

public class Server {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(12345);) {
            System.out.println("Socket server is running on " + serverSocket.getInetAddress().getHostAddress() + " post " + serverSocket.getLocalPort());
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Connecting client on address " + socket.getInetAddress() + " post " + socket.getPort());
                ServerThread serverThread = new ServerThread(socket, "");
                serverThread.start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
