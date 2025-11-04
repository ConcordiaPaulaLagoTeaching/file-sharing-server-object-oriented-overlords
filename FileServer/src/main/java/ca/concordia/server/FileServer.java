package ca.concordia.server;

import ca.concordia.filesystem.FileSystemManager;
import ca.concordia.filesystem.datastructures.FEntry;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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
        FileSystemManager manager = null;
        try {
            manager = new FileSystemManager(diskFile, totalSize);
        } catch (Exception e) {
            System.err.println("Error initializing FileSystemManager: " + e.getMessage());
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

    public static class ClientHandler implements Runnable {

        private final Socket clientSocket;
        private final FileSystemManager fsManager;

        public ClientHandler(Socket clientSocket, FileSystemManager fsManager) {
            this.clientSocket = clientSocket;
            this.fsManager = fsManager;
        }

        @Override
        public void run() {
            try (
                    BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    PrintWriter output = new PrintWriter(clientSocket.getOutputStream(), true)
            ) {
                output.println("Welcome to the File Server!");
                output.println("Commands: CREATE <file>, WRITE <file> <data>, READ <file>, DELETE <file>, LIST, EXIT");
                boolean running = true;
                while (running) {
                    String command = input.readLine();
                    if (command == null) break;
                    String[] parts = command.trim().split(" ", 3);
                    String cmd = parts[0].toUpperCase();
                    try {
                        switch (cmd) {
                            case "CREATE":
                                if (parts.length < 2) {
                                    output.println("Usage: CREATE <filename>");
                                    break;
                                }
                                fsManager.createFile(parts[1]);
                                output.println("File created: " + parts[1]);
                                break;
                            case "WRITE":
                                if (parts.length < 3) {
                                    output.println("Usage: WRITE <filename> <data>");
                                    break;
                                }
                                fsManager.writeFile(parts[1], parts[2]);
                                output.println("Wrote data to: " + parts[1]);
                                break;
                            case "READ":
                                if (parts.length < 2) {
                                    output.println("Usage: READ <filename>");
                                    break;
                                }
                                String content = fsManager.readFile(parts[1]);
                                output.println("File content: " + content);
                                break;
                            case "DELETE":
                                if (parts.length < 2) {
                                    output.println("Usage: DELETE <filename>");
                                    break;
                                }
                                fsManager.deleteFile(parts[1]);
                                output.println("File deleted: " + parts[1]);
                                break;
                            case "LIST":
                                output.println("Listing files:");
                                output.println(listFilesToString(fsManager));
                                output.println("(End of list)");
                                break;
                            case "EXIT":
                                output.println("Closing connection...");
                                running = false;
                                break;
                            default:
                                output.println("Unknown command: " + cmd);
                                break;
                        }
                    } catch (Exception e) {
                        output.println("Error: " + e.getMessage());
                    }
                }
                clientSocket.close();
                System.out.println("Client disconnected: " + clientSocket.getInetAddress());
            } catch (Exception e) {
                System.err.println("Client handling error: " + e.getMessage());
            }
        }

        private static String listFilesToString(FileSystemManager fsManager) {
            StringBuilder sb = new StringBuilder();
            try {
                java.lang.reflect.Field inodeField = FileSystemManager.class.getDeclaredField("inodeTable");
                inodeField.setAccessible(true);
                FEntry[] inodeTable = (FEntry[]) inodeField.get(fsManager);
                for (FEntry entry : inodeTable) {
                    if (entry != null) {
                        sb.append(" - ").append(entry.getFilename())
                                .append(" (").append(entry.getFilesize()).append(" bytes)\n");
                    }
                }
            } catch (Exception e) {
                sb.append("Error listing files: ").append(e.getMessage());
            }
            return sb.toString();
        }
    }
}
