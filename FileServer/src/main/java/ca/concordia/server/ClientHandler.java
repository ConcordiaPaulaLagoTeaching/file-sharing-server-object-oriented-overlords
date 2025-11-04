package ca.concordia.server;

import ca.concordia.filesystem.FileSystemManager;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {

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
                            output.println(fsManager.listFilesToString());
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
}
