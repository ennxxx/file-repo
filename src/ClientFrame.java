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

public class ClientFrame {

    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 12345;
    private boolean isConnected = false;
    private Socket soc;
    private BufferedReader in;
    private PrintWriter out;

    private JFrame frame;
    private JTextPane textPane;
    private JTextField inputField;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ClientFrame();
        });
    }

    public ClientFrame() {
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

        // Opening message when the client starts
        textPane.setText("Client started...\n\n[/join <server_ip_add> <port>]\t: Connect to a server application\n");
        appendToTextArea("[/?]\t\t\t: Request command help");

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

    private void connectToServer(String userInput) {
        try {
            soc = new Socket(SERVER_IP, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(soc.getInputStream()));
            out = new PrintWriter(soc.getOutputStream(), true);

            // Connection verification
            appendToTextArea(in.readLine());
            isConnected = true;

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
            } else if (message.startsWith("Server says:")) {
                StyleConstants.setForeground(style, Color.BLUE);
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

    // Process input
    private void processInput(String userInput) {
        String[] parts = userInput.split(" ");
        inputField.setText("");

        if ("/leave".equals(parts[0])) {
            try {
                if (isConnected) {
                    soc.close();
                    in.close();
                    out.close();
                    appendToTextArea("Server says: Connection closed. Thank you!");
                    isConnected = false;
                } else {
                    appendToTextArea("Error: Disconnection failed. Please connect to the server first");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if ("/?".equals(parts[0])) {
            appendToTextArea("Available commands:\n/join <server_ip_add> <port>\n/leave\n/register <handle>\n/store <filename>\n/dir\n/get <filename>\n/?");
        } else if (!("/join".equals(parts[0])) || parts.length != 3) {
            appendToTextArea("Error: Command parameters do not match or is not allowed.");
        } else if ("/register".equals(parts[0]) || "/store".equals(parts[0]) || "/dir".equals(parts[0]) || "/get".equals(parts[0])) {
            
            try {
                out.println(userInput);
                String serverResponse;
                
                while ((serverResponse = in.readLine()) != null) {
                    appendToTextArea(serverResponse);
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            
        } else {
            String serverIp = parts[1];
            int port;
            try {
                port = Integer.parseInt(parts[2]);

                // Checking for a valid server IP and port
                if (!(serverIp.equals(SERVER_IP) && port == SERVER_PORT)) {
                    appendToTextArea("Error: Connection to the Server has failed! Please check IP Address and Port Number.");
                } else {
                    connectToServer(userInput);
                }
            } catch (NumberFormatException e) {
                // Handle the case where the port is not a valid integer
                appendToTextArea("Error: Invalid port number. Please provide a valid integer for the port.");
            }
        }
    }
}
