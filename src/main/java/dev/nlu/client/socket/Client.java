package dev.nlu.client.socket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {
    public static void main(String[] args) {
        try(Socket socket = new Socket("localhost", 12345);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());) {
            System.out.println(in.readUTF());

        } catch (UnknownHostException e) { // IP
            throw new RuntimeException(e);
        } catch (IOException e) { // Port
            throw new RuntimeException(e);
        }
    }
}
