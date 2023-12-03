import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class FileExchangeClient {
    public static void main(String[] args) {
        try {

            // Establish connection with the server
            System.out.println("Client started...");

            String[] parts;
            boolean isValid;
            do {
                isValid = true;

                // Input Syntax: /join <server_ip_add> <port>
                System.out.print("Enter command (e.g., /join 127.0.0.1 12345): ");
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                String input = reader.readLine();
                parts = input.split(" ");

                // Request for Syntax commands
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
                } else if (!input.startsWith("/join") || parts.length != 3) {
                    System.out.println("Error: Command parameters do not match or is not allowed.");
                    return;
                } else {
                    String serverIp = parts[1];
                    int port = Integer.parseInt(parts[2]);

                    // Checking for valid port and IP
                    if (!(serverIp.equals("127.0.0.1") && port == 12345)) {
                        System.out.println("Error: Connection to the Server has failed! Please check IP Address and Port Number.");
                        isValid = false;
                    }

                }
            } while(!isValid);


            String serverIp = parts[1];
            int port = Integer.parseInt(parts[2]);
            Socket soc = new Socket(serverIp, port);
            BufferedReader in = new BufferedReader(new InputStreamReader(soc.getInputStream()));
            System.out.println(in.readLine()); //Connection verification

            // TODO: Handle client communication or file transfer here

            // Create input and output streams for communication with the server
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
            PrintWriter out = new PrintWriter(soc.getOutputStream(), true);

            // Read user input and send it to the server
            String userInputString;
            while (true) {
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
            }

            // Close resources
            userInput.close();
            in.close();
            out.close();
            soc.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
