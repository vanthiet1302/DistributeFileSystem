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
            System.out.println("Socket server is running on "
                    + serverSocket.getInetAddress().getHostAddress()
                    + " post "
                    + serverSocket.getLocalPort());
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Connecting client on address "
                        + socket.getInetAddress()
                        + " post "
                        + socket.getPort());
                new Thread(() -> threadProcess(socket)).start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void threadProcess(Socket socket) {
        try (socket;
             BufferedReader reader =
                     new BufferedReader(new InputStreamReader(socket.getInputStream()));

             PrintWriter writer =
                     new PrintWriter(socket.getOutputStream(), true);

             DataInputStream dis =
                     new DataInputStream(socket.getInputStream());

             DataOutputStream dos =
                     new DataOutputStream(socket.getOutputStream());
        ) {

            writer.println("Hello! You are connecting to Socket server");

            String defaultPath = "/home/vvt/TMP";
            File defaultDir = new File(defaultPath);
            long maxFileLength = 1024 * 1024 * 100; // 100 MB
            User curUser = null;
            String username = "";

            while (curUser == null) {
                String request = reader.readLine();
                if (request.isBlank()) continue;

                String[] split = request.split("\\s+", 2);
                String command = split[0].toUpperCase();
                switch (command) {
                    case "EXIT" -> {
                        writer.println("Exited.");
                        System.out.println("Closed connection client "
                                + socket.getInetAddress()
                                + " port "
                                + socket.getPort());
                        return;
                    }
                    case "LOGIN" -> {
                        if (split.length < 2) {
                            writer.println("ERROR: Command LOGIN <USERNAME>");
                            continue;
                        }
                        username = split[1];
                    }
                    case "PASS" -> {
                        if (split.length < 2) {
                            writer.println("ERROR: Command PASS <PASSWORD>");
                            continue;
                        }
                        String pass = split[1];
                        Optional<User> validate = DBUtils.validateUser(username, pass);
                        if (validate.isPresent()) {
                            curUser = validate.get();
                        }
                    }
                    default -> {
                        writer.println("Command " + request + " invalid.");
                    }
                }
            }

            System.out.println("User " + curUser.getUsername() + " was log in");
            writer.println("Hello " + username);
            boolean connected = true;

            while (connected) {
                String request = reader.readLine();
                if (request.isBlank()) continue;

                String[] split = request.split("\\s+", 2);
                String command = split[0].toUpperCase();
                switch (command) {
                    case "EXIT" -> {
                        writer.println("Google bye!");
                        System.out.println("Closed connection client "
                                + socket.getInetAddress()
                                + " port "
                                + socket.getPort());
                        return; // connected = false
                    }
                    case "UPLOAD" -> { // upload fileName fileSize [newName] -> ok
                        if (split.length < 2) {
                            writer.println("ERROR: Command UPLOAD <fileName> [newName]");
                            continue;
                        }
                        String[] params = split[1].split("\\s+", 3);
                        if (params.length < 2) {
                            writer.println("ERROR: Client miss request file size.");
                            continue;
                        }
                        String fileName = params[0];
                        long fileSize = Long.parseLong(params[1]);

                        File upload = new File(defaultDir, fileName).getCanonicalFile();
                        if (!upload.getPath().startsWith(defaultDir.getPath())) {
                            writer.println("ERROR: Invalid path.");
                            continue;
                        }
                        if (fileSize > maxFileLength) {
                            writer.println("ERROR: Max file size is " + maxFileLength);
                            continue;
                        }
                        writer.println("READY");
                        try (DataOutputStream upStream = new DataOutputStream(new FileOutputStream(upload, false))) {
                            copy(dis, upStream, fileSize);
                        }
                        writer.println("OK");

                    }
                    case "DOWNLOAD" -> { // download filename [newName] -> ok -> fileSize

                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void copy(InputStream in, OutputStream out, long size) throws IOException {
        long remaining = size;
        byte[] buffer = new byte[100 * 1024];
        int mustBytesRead = 0;
        while (remaining > 0) {
            mustBytesRead = in.read(buffer, 0, (int) Math.min(remaining, buffer.length));
            if (mustBytesRead == -1) break;
            out.write(buffer, 0, mustBytesRead);
            remaining -= mustBytesRead;
        }
    }
}
