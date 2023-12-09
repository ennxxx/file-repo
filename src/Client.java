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
        System.out.println("\u001B[32mClient started...\u001B[0m");

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
                    System.out.println("\u001B[32mAvailable commands:\n/join <server_ip_add> <port>\n/leave\n/register <handle>\n/store <filename>\n/dir\n/get <filename>\n/?" + "\u001B[0m");
                    isValid = false;
                } else if (input.equals("/leave")) {
                    System.out.println("\u001B[31mError: Disconnection failed. Please connect to the server first.\u001B[0m");
                    isValid = false;
                } else if (!input.startsWith("/join") || parts.length != 3) {
                    System.out.println("\u001B[31mError: Command parameters do not match or are not allowed.\u001B[0m");
                    isValid = false;
                } else {
                    String serverIp = parts[1];
                    int port;
                    try {
                        port = Integer.parseInt(parts[2]);

                        // Checking for a valid server IP and port
                        if (!(serverIp.equals(SERVER_IP) && port == SERVER_PORT)) {
                            System.out.println("\u001B[31mError: Connection to the Server has failed! Please check IP Address and Port Number.\u001B[0m");
                            isValid = false;
                        }
                    } catch (NumberFormatException e) {
                        // Handle the case where the port is not a valid integer
                        System.out.println("\u001B[31mError: Invalid port number. Please provide a valid integer for the port.\u001B[0m");
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
                        System.exit(1);
                    }
                }).start();

                System.out.println("\u001B[32m\nYou can now interact with the server!");
                System.out.println("(Type \"/?\" to check for the commands)");
                System.out.println("--------------------------------------\u001B[0m");


                // Start a thread to read user input and send it to the server
                BufferedReader userInputReader = new BufferedReader(new InputStreamReader(System.in));
                while (true) {
                    try {
                        // Print a prompt for the user
                        String userInputString = userInputReader.readLine();

                        out.writeUTF(userInputString);

                        // User wants to check all commands
                        if ("/?".equals(userInputString)) {
                            System.out.println("\u001B[32mAvailable commands:\n/join <server_ip_add> <port>\n/leave\n/register <handle>\n/store <filename>\n/dir\n/get <filename>\n/?" + "\u001B[0m");
                        }
                        // Check if the user wants to leave the server
                        else if ("/leave".equals(userInputString)) {
                            break; // exit the loop when leaving the server
                        }
                    } catch (IOException e) {
                        // Handle communication error with the server
                        System.out.println("\u001B[31mError: Communication with the server failed. Exiting...\u001B[0m");
                        System.exit(1);
                    }
                }
            } catch (IOException e) {
                System.out.println("\u001B[31mError: Unable to connect to the server. Please make sure the server is running.\u001B[0m");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
