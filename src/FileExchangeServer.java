import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileExchangeServer {

    // Map to store handles (alias) associated with client sockets
    private static final Map<String, Socket> clientHandles = new HashMap<>();
    private static final String SERVER_DIRECTORY = "../ServerDirectory";
    private static final String CLIENT_DIRECTORY = "../ClientDirectory";

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
                    registerHandle(command, out, clientSocket);
                } else if (command.startsWith("/store")) {
                    sendFile(command, out, clientSocket);  // Pass clientSocket here
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
    private static void registerHandle(String command, PrintWriter out, Socket clientSocket) {
        // Extract the handle/alias from the command
        String[] tokens = command.split("\\s+");

        if (tokens.length == 2) {
            String handle = tokens[1];

            // Check if the handle is already registered
            if (clientHandles.containsKey(handle)) {
                out.println("Error: Registration failed. Handle or alias already exists.");
            } else {
                // Register the handle
                clientHandles.put(handle, clientSocket);
                out.println("Welcome " + handle + "!");
            }
        } 
    }

    private static String getClientHandle(Socket clientSocket) {
        // Find the handle associated with the clientSocket
        for (Map.Entry<String, Socket> entry : clientHandles.entrySet()) {
            if (entry.getValue() == clientSocket) {
                return entry.getKey();
            }
        }
        return "Unknown";
    }
    
    private static void broadcastMessage(String message, Socket senderSocket) {
        // Send the message to all connected clients, excluding the sender
        for (Map.Entry<String, Socket> entry : clientHandles.entrySet()) {
            if (!entry.getValue().equals(senderSocket)) {
                try {
                    PrintWriter socketOut = new PrintWriter(entry.getValue().getOutputStream(), true);
                    socketOut.println(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    /* TODO: Send file to server */
    private static void sendFile(String command, PrintWriter out, Socket clientSocket) {
        // Extract the filename from the command
        String[] tokens = command.split("\\s+");
    
        if (tokens.length == 2) {
            String filename = tokens[1];
            Path clientFilePath = Paths.get(CLIENT_DIRECTORY, filename);
            Path serverFilePath = Paths.get(SERVER_DIRECTORY, filename);
    
            if (Files.exists(clientFilePath)) {
                try {
                    // Read file content
                    byte[] fileContent = Files.readAllBytes(clientFilePath);
    
                    // Write file content to server directory
                    Files.write(serverFilePath, fileContent);
    
                    // Get current timestamp
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String timestamp = dateFormat.format(new Date());
    
                    // Broadcast the message to all connected clients
                    String message = String.format("%s<%s>: Uploaded %s.", getClientHandle(clientSocket), timestamp, filename);
                    broadcastMessage(message, clientSocket);
    
                    out.println(message);
                } catch (IOException e) {
                    e.printStackTrace();
                    out.println("Error: Unable to send the file.");
                }
            } else {
                out.println("Error: File not found on the client side.");
            }
        } 
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
            out.println("Server Directory: " + String.join(", ", fileNames));
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
                    out.println("File received from Server: " + filename);
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
