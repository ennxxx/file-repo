import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class FileExchangeServer {

    // Map to store handles (alias) associated with client sockets
    private static final Map<String, Socket> clientHandles = new HashMap<>();
    private static final String SERVER_DIRECTORY = "ServerDirectory";
    private static final String CLIENT_DIRECTORY = "ClientDirectory";

    public static void main(String[] args) {
        try {
            System.out.println("File Exchange Server is running...");

            ServerSocket serverSocket = new ServerSocket(12345);

            while (true) {
                // Wait for a client to connect
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getRemoteSocketAddress());

                // Handle client communication in a separate thread
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try {
            // Create input and output streams for communication with the client
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            // Initial greeting message to the client
            out.println("Server says: Connection to the File Exchange Server is Successful");

            // Handle client commands in a loop
            String command;
            while ((command = in.readLine()) != null) {
                if (command.startsWith("/leave")) {
                    out.println("Server says: Connection closed. Thank you!");
                } else if (command.startsWith("/register")) {
                    registerHandle();
                } else if (command.startsWith("/store")) {
                    sendFile();
                } else if (command.startsWith("/dir")) {
                    requestDirectory(out);
                } else if (command.startsWith("/get")) {
                    fetchFile(command, out);
                } else if (command.startsWith("/?")) {
                    out.println();
                } else {
                    out.println("Error: Command not found.");
                }
            }

            // Client has disconnected
            out.println("Server says: Connection closed. Thank you!");
            System.out.println("Client disconnected: " + clientSocket.getRemoteSocketAddress());
            in.close();
            out.close();
            clientSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* TODO: Register a unique handle or alias */
    private static void registerHandle() {
        // Implement logic to handle user registration
        // Extract the handle/alias from the command
        // ...
    }

    /* TODO: Send file to server */
    private static void sendFile() {
        // Implement logic to handle storing a file
        // Extract the filename from the command
        // ...
    }

    /* TODO: Request directory file list from a server */
    private static void requestDirectory(PrintWriter out) {
        File serverDirectory = new File(SERVER_DIRECTORY);
        File[] files = serverDirectory.listFiles();

        if (files != null) {
            String[] fileNames = new String[files.length];
            for (int i = 0; i < files.length; i++) {
                fileNames[i] = files[i].getName();
            }
            out.println("Files: " + String.join(", ", fileNames));
        } else {
            out.println("Server says: No files in the server directory.");
        }
    }

    /* TODO: Fetch a file from a server */
    private static void fetchFile(String command, PrintWriter out) {
        String[] tokens = command.split("\\s+");
        if (tokens.length == 2) {
            String filename = tokens[1];
            Path serverFilePath = Paths.get(SERVER_DIRECTORY, filename);
            Path clientFilePath = Paths.get(CLIENT_DIRECTORY, filename);

            if (Files.exists(serverFilePath)) {
                try {
                    byte[] fileContent = Files.readAllBytes(serverFilePath);
                    Files.write(clientFilePath, fileContent);
                    clientFilePath.toFile().getParentFile().list();
                    out.println("File successfully fetched and saved to ClientDirectory");
                } catch (IOException e) {
                    e.printStackTrace();
                    out.println("Error: Unable to fetch or save the file.");
                }
            } else {
                out.println("Error: File not found on the server.");
            }
        } else {
            out.println("Error: Command parameters do not match or is not allowed.");
        }
    }
}
