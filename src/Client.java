import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {

    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 12345;

    private JFrame frame;
    private JTextPane textPane;
    private JTextField inputField;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new Client();
        });
    }

    public Client() {
        initializeGUI();
    }

    private void initializeGUI() {
        frame = new JFrame("Client");
        frame.setSize(500, 600);
        frame.setResizable(false);
        frame.setLayout(new BorderLayout());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        textPane = new JTextPane();
        textPane.setEditable(false);

        // Establish connection with the server
        appendToTextArea("Client started...");

        JScrollPane scrollPane = new JScrollPane(textPane);
        frame.add(scrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        inputField = new JTextField();
        inputField.setPreferredSize(new Dimension(400, 30));
        inputPanel.add(inputField);

        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(e -> processInput(inputField.getText()));
        inputPanel.add(sendButton);

        frame.add(inputPanel, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private void establishConnection() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            appendToTextArea(in.readLine());  // Connection verification
        } catch (IOException e) {
            appendToTextArea("Error: Unable to connect to the server. Please make sure the server is running.");
        }
    }

    private void appendToTextArea(String message) {
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = textPane.getStyledDocument();
            Style style = textPane.addStyle("Style", null);

            if (message.startsWith("Error:")) {
                StyleConstants.setForeground(style, Color.RED);
            } else {
                StyleConstants.setForeground(style, Color.BLACK);
            }

            try {
                doc.insertString(doc.getLength(), message + "\n\n", style);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    private void processInput(String userInput) {
        String[] parts = userInput.split(" ");
        inputField.setText("");

        if (parts[0].equals("/?")) {
            appendToTextArea("Available commands:\n/join <server_ip_add> <port>\n/leave\n/register <handle>\n/store <filename>\n/dir\n/get <filename>\n/?");
        } else if (parts[0].equals("/leave")) {
            appendToTextArea("Error: Disconnection failed. Please connect to the server first.");
        } else if (!parts[0].startsWith("/join") || parts.length != 3) {
            appendToTextArea("Error: Command parameters do not match or are not allowed.");
        } else {
            String serverIp = parts[1];
            int port;
            try {
                port = Integer.parseInt(parts[2]);
                // Checking for a valid server IP and port
                if (!(serverIp.equals(SERVER_IP) && port == SERVER_PORT)) {
                    appendToTextArea("Error: Connection to the Server has failed! Please check IP Address and Port Number.");
                } else {
                    // Successfully connects to the server
                    establishConnection();
                    processUserInput(userInput);
                }
            } catch (NumberFormatException e) {
                // Handle the case where the port is not a valid integer
                appendToTextArea("Error: Invalid port number. Please provide a valid integer for the port.");
            }
        }
    }

    private void processUserInput(String userInput) {
        try {
            // Read user input and send it to the server
            out.println(userInput);
            System.out.println(userInput);

            // User wants to check all commands
            if ("/?".equals(userInput)) {
                appendToTextArea("Available commands:\n/join <server_ip_add> <port>\n/leave\n/register <handle>\n/store <filename>\n/dir\n/get <filename>\n/?");
                return;
            }

            // Check if the user wants to leave the server
            if ("/leave".equals(userInput)) {
                String serverResponse = in.readLine();
                appendToTextArea(serverResponse);
                return;
            }

            String serverResponse;
            while ((serverResponse = in.readLine()) != null) {
                appendToTextArea(serverResponse);
                System.out.println(serverResponse);
                return;
            }

        } catch (IOException e) {
            // Handle communication error with the server
            appendToTextArea("Error: Communication with the server failed. Exiting...");
        }
    }
}
