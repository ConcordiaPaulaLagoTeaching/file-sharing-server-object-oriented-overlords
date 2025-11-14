package ca.concordia.server;

import ca.concordia.filesystem.FileSystemManager;
import java.net.ServerSocket;
import java.net.Socket;

public class FileServer {

    private final int port;
    private final FileSystemManager fsManager;

    public FileServer() {
        this(12345, "virtual_disk.bin", 4096);
    }

    public FileServer(int port, String diskFile, int totalSize) {
        this.port = port;
        FileSystemManager manager;
        try {
            manager = new FileSystemManager(diskFile, totalSize);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize file system", e);
        }
        fsManager = manager;
        startServer();
    }

    private void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("File server started on port " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());
                ClientHandler handler = new ClientHandler(clientSocket, fsManager);
                new Thread(handler).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new FileServer();
    }
}
