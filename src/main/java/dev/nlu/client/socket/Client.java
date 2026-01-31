package dev.nlu.client.socket;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 12345);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream());
             BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));) {
            System.out.println(dis.readUTF()); // Chào
            String line = null;
            boolean isBreak = false;
            while ((line = userInput.readLine()) != null) {
                line = line.trim();

                if ("exit".equalsIgnoreCase(line)) {
                    isBreak = true;
                    dos.writeUTF("exit");
                    dos.flush();
                    break;
                }

                String[] parts = line.split("\\s+", 2);
                String command = parts[0].toLowerCase();
                String response = "";
                switch (command) {
                    case "login" -> {
                        if (parts.length != 2) {
                            continue;
                        }

                        String[] params = parts[1].split("\\s+", 2);
                        if (params.length != 2) {
                            continue;
                        }

                        dos.writeUTF(line);
                        dos.flush();
                        response = dis.readUTF(); // ok or error
                        System.out.println(response);
                    }

                    case "ls" -> {
                        if (parts.length != 1) {
                            continue;
                        }
                        dos.writeUTF(line);
                        dos.flush();
                        response = dis.readUTF(); // file1.txt:10MB;\nfile2.jpg:2MB
                        System.out.println(response);
                    }

                    case "upload" -> {
                        if (parts.length != 2) {
                            continue;
                        }
                        String filePath = parts[1];
                        File file = new File(filePath);
                        long fileSize = file.length();
                        if (!file.exists() || !file.isFile()) {
                            continue;
                        }

                        dos.writeUTF(line + " " + fileSize);
                        dos.flush();
                        response = dis.readUTF();
                        if ("ok".equalsIgnoreCase(response)) {
                            try (DataInputStream input = new DataInputStream(new FileInputStream(file))) {
                                int bytesRead = 0;
                                byte[] buffer = new byte[100 * 1024];
                                while ((bytesRead = input.read(buffer)) != -1) {
                                    dos.write(buffer, 0, bytesRead);
                                    dos.flush();
                                }
                            }
                        } else {
                            System.out.println(response);
                            continue;
                        }

                        response = dis.readUTF();
                        if ("ok".equalsIgnoreCase(response)) {
                            System.out.println(response);
                        } else {
                            System.out.println(response);
                        }
                    }

                    case "download" -> {
                        if (parts.length != 2) {
                            continue;
                        }

                        String fileName = parts[1];
                        File file = new File(fileName);
                        if (file.exists() && file.isFile()) {
                            System.out.println("file exists.");
                            continue;
                        }

                        dos.writeUTF("ok");
                        dos.flush();
                        try (DataOutputStream output = new DataOutputStream(new FileOutputStream(file))) {
                            int bytesRead = 0;
                            byte[] buffer = new byte[100 * 1024];
                            while ((bytesRead = dis.read(buffer)) != -1) {
                                output.write(buffer, 0, bytesRead);
                            }
                        }

                        response = dis.readUTF();
                        if ("ok".equalsIgnoreCase(response)) {
                            System.out.println(response);
                        } else if ("error".equalsIgnoreCase(response)) {
                            System.out.println(response);
                        }
                    }
                }

                if (isBreak) {
                    break;
                }
            }

            System.out.println(dis.readUTF()); // Xin chao, hen gap lai
        } catch (UnknownHostException e) { // IP
            throw new RuntimeException(e);
        } catch (IOException e) { // Port
            throw new RuntimeException(e);
        }
    }
}