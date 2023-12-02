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

            // Input Syntax: /join <server_ip_add> <port>
            System.out.print("Enter server IP address and port (e.g., /join 127.0.0.1 9806): ");
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String input = reader.readLine();

            // Checks for valid "/join" command
            if (!input.startsWith("/join")) {
                System.out.println("Invalid syntax. Use /join <server_ip_add> <port>");
                return;
            }

            // Checks for valid "/join" parameters
            String[] parts = input.split(" ");
            if (parts.length != 3) {
                System.out.println("Invalid syntax. Use /join <server_ip_add> <port>");
                return;
            }

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
                    break; // Assuming one response per command for simplicity
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
