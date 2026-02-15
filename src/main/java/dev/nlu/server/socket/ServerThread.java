package dev.nlu.server.socket;

import dev.nlu.model.User;
import dev.nlu.utils.DBUtils;

import java.io.*;
import java.net.Socket;
import java.util.Optional;

public class ServerThread extends Thread {
    private final static long MAX_FILE_LENGTH = 100 * 1024 * 1024; // 100 MB
    private User curUser;
    private Socket socket;
    private String defaultPath;
    private String lastUsername;
    private DataInputStream dis;
    private DataOutputStream dos;

    public ServerThread(Socket socket, String defaultPath) {
        this.socket = socket;
        this.defaultPath = defaultPath;
        try {
            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        try {
            dos.writeUTF("Hello! You are connecting to Socket server");
            dos.flush();

            while (curUser == null) {
                String request = dis.readUTF();
                if (request.isBlank()) continue;
                if ("EXIT".equalsIgnoreCase(request)) return;
                loginController(request);
            }

            System.out.println("User " + curUser.getUsername() + " was log in");
            dos.writeUTF("Hello " + lastUsername);

            while (true) {
                String request = dis.readUTF();
                if (request.isBlank()) continue;
                if (!fileController(request)) break;
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            closeConnection();
        }
    }

    private void loginController(String request) throws IOException {
        String[] split = request.split("\\s+", 2);
        String command = split[0].toUpperCase();
        switch (command) {
            case "EXIT" -> {
                dos.writeUTF("Exited.");
                dos.flush();
                System.out.println("Closed connection client " + socket.getInetAddress() + " port " + socket.getPort());
            }
            case "LOGIN" -> {
                if (split.length < 2) {
                    dos.writeUTF("ERROR: Command LOGIN <USERNAME>");
                    dos.flush();
                }
                this.lastUsername = split[1];
            }
            case "PASS" -> {
                if (split.length < 2) {
                    dos.writeUTF("ERROR: Command PASS <PASSWORD>");
                    dos.flush();
                }
                String pass = split[1];
                Optional<User> validate = DBUtils.validateUser(this.lastUsername, pass);
                if (validate.isPresent()) {
                    curUser = validate.get();
                }
            }
            default -> {
                dos.writeUTF("Command " + request + " invalid.");
                dos.flush();
            }
        }
    }

    private boolean fileController(String request) throws IOException {
        String[] split = request.split("\\s+", 2);
        String command = split[0].toUpperCase();

        switch (command) {
            case "EXIT" -> {
                dos.writeUTF("Goodbye! See you later.");
                dos.flush();
                return false;
            }
            case "UPLOAD" -> {
                handleUpload(split);
                return true;
            }
            case "DOWNLOAD" -> {
                handleDownload(split);
                return true;
            }
            default -> {
                dos.writeUTF("Command invalid.");
                dos.flush();
                return true;
            }
        }
    }

    private void handleDownload(String[] split) throws IOException {
        if (split.length < 2) {
            dos.writeUTF("ERROR: Command DOWNLOAD <fileName>");
            dos.flush();
            return;
        }

        String fileName = split[1].trim();

        File baseDir = new File(defaultPath).getCanonicalFile();
        File downloadFile = new File(baseDir, fileName).getCanonicalFile();

        if (!downloadFile.getPath().startsWith(baseDir.getPath())) {
            dos.writeUTF("ERROR: Invalid path.");
            dos.flush();
            return;
        }

        if (!downloadFile.exists() || !downloadFile.isFile()) {
            dos.writeUTF("ERROR: File not found.");
            dos.flush();
            return;
        }

        long fileSize = downloadFile.length();
        dos.writeUTF("OK " + fileSize);
        dos.flush();

        String clientResponse = dis.readUTF();
        if ("READY".equalsIgnoreCase(clientResponse)) {
            try (FileInputStream fis = new FileInputStream(downloadFile)) {
                copy(fis, dos, fileSize);
            }
            dos.flush();
            System.out.println("User " + curUser.getUsername() + " downloaded file: " + fileName);
        } else {
            System.out.println("Download aborted by client.");
        }
    }

    private void handleUpload(String[] split) throws IOException {
        if (split.length < 2) {
            dos.writeUTF("ERROR: Command UPLOAD <fileName> <fileSize>");
            return;
        }

        String[] params = split[1].split("\\s+");
        if (params.length < 2) {
            dos.writeUTF("ERROR: Missing file size.");
            return;
        }

        String fileName = params[0];
        long fileSize = Long.parseLong(params[1]);

        File baseDir = new File(defaultPath).getCanonicalFile();
        File uploadFile = new File(baseDir, fileName).getCanonicalFile();

        if (!uploadFile.getPath().startsWith(baseDir.getPath())) {
            dos.writeUTF("ERROR: Invalid path.");
            return;
        }
        if (fileSize > MAX_FILE_LENGTH) {
            dos.writeUTF("ERROR: File too large.");
            return;
        }

        dos.writeUTF("READY");
        dos.flush();

        try (FileOutputStream fos = new FileOutputStream(uploadFile)) {
            copy(dis, fos, fileSize);
        }

        dos.writeUTF("OK");
        dos.flush();
    }

    private void copy(InputStream in, OutputStream out, long size) throws IOException {
        long remaining = size;
        byte[] buffer = new byte[1024 * 64];
        while (remaining > 0) {
            int read = in.read(buffer, 0, (int) Math.min(remaining, buffer.length));
            if (read == -1) throw new EOFException("Stream closed prematurely");
            out.write(buffer, 0, read);
            remaining -= read;
        }
    }

    private void closeConnection() {
        try {
            if (dis != null) dis.close();
            if (dos != null) dos.close();
            if (socket != null) socket.close();
            System.out.println("Closed connection.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
