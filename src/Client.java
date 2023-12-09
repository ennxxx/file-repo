import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class Client {

    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        System.out.println("Client started...");

        try {
            // Establish connection with the server
            String[] parts;
            boolean isValid;
            do {

                // Input Syntax: /join <server_ip_add> <port>
                System.out.print("Enter command (e.g., /join 127.0.0.1 12345): ");
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                String input = reader.readLine();
                parts = input.split(" ");

                // Request for Syntax commands
                isValid = true;
                if (input.equals("/?")) {
                    System.out.print("""
                            Available commands:
                            /join <server_ip_add> <port>
                            /leave
                            /register <handle>
                            /store <filename>
                            /dir
                            /get <filename>
                            /?
                            """);
                    isValid = false;
                } else if (input.equals("/leave")) {
                    System.out.println("Error: Disconnection failed. Please connect to the server first.");
                    isValid = false;
                } else if (!input.startsWith("/join") || parts.length != 3) {
                    System.out.println("Error: Command parameters do not match or is not allowed.");
                    isValid = false;
                } else {
                    String serverIp = parts[1];
                    int port;
                    try {
                        port = Integer.parseInt(parts[2]);

                        // Checking for a valid server IP and port
                        if (!(serverIp.equals(SERVER_IP) && port == SERVER_PORT)) {
                            System.out.println("Error: Connection to the Server has failed! Please check IP Address and Port Number.");
                            isValid = false;
                        }
                    } catch (NumberFormatException e) {
                        // Handle the case where the port is not a valid integer
                        System.out.println("Error: Invalid port number. Please provide a valid integer for the port.");
                        isValid = false;
                    }

                }
            } while (!isValid);

            String serverIp = parts[1];
            int port = Integer.parseInt(parts[2]);
            Socket soc = null;

            try {
                soc = new Socket(serverIp, port);
                DataInputStream in = new DataInputStream(soc.getInputStream());
                System.out.println(in.readUTF()); // Connection verification

                DataOutputStream out = new DataOutputStream(soc.getOutputStream());

                // Start a thread to continuously listen to server messages
                new Thread(() -> {
                    try {
                        while (true) {
                            String serverResponse = in.readUTF();
                            System.out.println(serverResponse);
                        }
                    } catch (IOException e) {
                        // Handle communication error with the server
                        System.out.println("Error: Communication with the server failed. Exiting...");
                        System.exit(1);
                    }
                }).start();

                // Start a thread to read user input and send it to the server
                BufferedReader userInputReader = new BufferedReader(new InputStreamReader(System.in));
                while (true) {
                    try {
                        // Print a prompt for the user
                        System.out.print("Enter command: ");
                        String userInputString = userInputReader.readLine();

                        out.writeUTF(userInputString);

                        // User wants to check all commands
                        if ("/?".equals(userInputString)) {
                            System.out.print("Available commands:\n/join <server_ip_add> <port>\n/leave\n/register <handle>\n/store <filename>\n/dir\n/get <filename>\n/?");
                        }

                        // Check if the user wants to leave the server
                        if ("/leave".equals(userInputString)) {
                            String serverResponse = in.readUTF();
                            System.out.println(serverResponse);
                            break;
                        }
                    } catch (IOException e) {
                        // Handle communication error with the server
                        System.out.println("Error: Communication with the server failed. Exiting...");
                        System.exit(1);
                    }
                }
            } catch (IOException e) {
                System.out.println("Error: Unable to connect to the server. Please make sure the server is running.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
