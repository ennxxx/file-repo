import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class FileExchangeServer {

    // Map to store handles (alias) associated with client sockets
    private static final Map<String, Socket> clientHandles = new HashMap<>();

    public static void main(String[] args) {
        try {
            System.out.println("File Exchange Server is running...");

            ServerSocket serverSocket = new ServerSocket(9806);

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
            String input;
            while ((input = in.readLine()) != null) {
                handleCommand(clientSocket, out, input);
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

    private static void handleCommand(Socket clientSocket, PrintWriter out, String command) {
        // Implement logic to handle different commands
        // You need to parse the command and perform corresponding actions
        // based on the provided syntax

        // Example: If the command is "/join <server_ip_add> <port>"
        if (command.startsWith("/leave")) {
            out.println("Server says: Connection closed. Thank you!");
        } else if (command.startsWith("/register")) {
            // Implement logic to handle user registration
            // Extract the handle/alias from the command
            // ...
        } else if (command.startsWith("/store")) {
            // Implement logic to handle storing a file
            // Extract the filename from the command
            // ...
        } else if (command.startsWith("/dir")) {
            // Implement logic to handle listing directory files
            // ...
        } else if (command.startsWith("/get")) {
            // Implement logic to handle fetching a file
            // Extract the filename from the command
            // ...
        } else if (command.startsWith("/?")) {
            out.println();
        } else {
            out.println("Error: Command not found.");
        }
    }
}
