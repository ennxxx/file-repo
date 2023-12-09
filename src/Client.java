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

        try (Socket soc = new Socket(SERVER_IP, SERVER_PORT)) {
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
    }
}
