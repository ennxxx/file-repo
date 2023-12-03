import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class FileExchangeClient {

    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        try {
            // Establish connection with the server
            System.out.println("Client started...");

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
            } while(!isValid);

            String serverIp = parts[1];
            int port = Integer.parseInt(parts[2]);
            Socket soc = null;
            try {
                soc = new Socket(serverIp, port);
                BufferedReader in = new BufferedReader(new InputStreamReader(soc.getInputStream()));
                System.out.println(in.readLine()); // Connection verification

                // TODO: Handle client communication or file transfer here

                // Create input and output streams for communication with the server
                BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
                PrintWriter out = new PrintWriter(soc.getOutputStream(), true);

                // Read user input and send it to the server
                String userInputString;
                while (true) {
                    try {
                        // Print a prompt for the user
                        System.out.print("Enter command: ");
                        userInputString = userInput.readLine();

                        out.println(userInputString);

                        // User wants to check all commands
                        if ("/?".equals(userInputString)) {
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
                        }

                        // Check if the user wants to leave the server
                        if ("/leave".equals(userInputString)) {
                            String serverResponse = in.readLine();
                            System.out.println(serverResponse);
                            break;
                        }

                        // Print server responses
                        String serverResponse;
                        while ((serverResponse = in.readLine()) != null) {
                            System.out.println(serverResponse);
                            break;
                        }
                    } catch (IOException e) {
                        // Handle communication error with the server
                        System.out.println("Error: Communication with the server failed. Exiting...");
                        break;
                    }
                }

                // Close resources
                userInput.close();
                in.close();
                out.close();
            } catch (IOException e) {
                System.out.println("Error: Unable to connect to the server. Please make sure the server is running.");
            } finally {
                if (soc != null && !soc.isClosed()) {
                    try {
                        soc.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
