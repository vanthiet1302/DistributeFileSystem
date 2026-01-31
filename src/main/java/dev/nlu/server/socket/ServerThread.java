package dev.nlu.server.socket;

import dev.nlu.model.User;
import dev.nlu.utils.DBUtils;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Optional;

public class ServerThread extends Thread {
    private Socket socket;
    private DBUtils dao;
    private File defaultFolder;

    public ServerThread(Socket socket) {
        this.socket = socket;
        dao = new DBUtils();
        defaultFolder = new File("D:\\TMP");
    }

    @Override
    public void run() {
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
             DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));) {
            dos.writeUTF("Connecting to server: " + socket.getInetAddress().getHostName());
            dos.flush();

            String line = null;
            boolean isBreak = false;
            while ((line = dis.readLine()) != null) {
                line = line.trim();

                if ("exit".equalsIgnoreCase(line)) {
                    dos.writeUTF("Good bye!" + socket.getInetAddress().getHostName());
                    dos.flush();
                    isBreak = true;
                }

                String parts[] = line.split("\\s+", 2);
                String command = parts[0].toLowerCase();
                String response = "";

                switch (command) {
                    case "login" -> { // them session sau
                        String[] params = parts[1].split("\\s+", 2);
                        if (params.length != 2) {
                            dos.writeUTF("error: login username password");
                            dos.flush();
                            continue;
                        }

                        Optional<User> user = dao.validateUser(params[0], params[1]);
                        if (user.isEmpty()) {
                            dos.writeUTF("error: username or password invalid");
                            dos.flush();
                            continue;
                        }

                        dos.writeUTF("ok");
                        dos.flush();
                    }

                    case "ls" -> {
                        StringBuilder sb = new StringBuilder();
                        File[] children = defaultFolder.listFiles();
                        if (children != null) {
                            for (File child : children) {
                                if (child.isFile()){
                                    sb.append(child.getName()).append("\n");
                                }

                                if (child.isDirectory()) {
                                    sb.append(child.getName().toUpperCase()).append("\n");
                                }
                            }
                        }

                        dos.writeUTF(sb.toString());
                        dos.flush();
                    }

                    case "upload" -> {
                        String params[] =  parts[1].split("\\s+", 2);
                        if (params.length != 2) {
                            dos.writeUTF("error: upload file name not correct");
                            dos.flush();
                            continue;
                        }

                        String fileName = params[0];
                        long fileSize = Long.parseLong(params[1]); // try catch numbser format ex
                        File file = new File(defaultFolder, fileName);
                        if (file.exists()) {
                            dos.writeUTF("error: file already exists");
                            dos.flush();
                            continue;
                        }

                        dos.writeUTF("ok");
                        dos.flush();
                        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
                            int bytesRead = 0;
                            byte[] buffer = new byte[100 * 1024];
                            while ((bytesRead = dis.read(buffer)) != -1) {
                                out.write(buffer, 0, bytesRead);
                            }
                        }

                        if (fileSize != file.length()) {
                            dos.writeUTF("error: file size not correct");
                            dos.flush();
                        }

                        dos.writeUTF("ok");
                        dos.flush();
                    }

                    case "download" -> {

                    }
                }


                if (isBreak) {
                    break;
                }
            }



        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
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
