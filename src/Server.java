import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Server {

    private static final Map<String, Socket> clientHandles = new HashMap<>();
    private static final String SERVER_DIRECTORY = "../ServerDirectory";
    private static final String CLIENT_DIRECTORY = "../ClientDirectory";

    public static void main(String[] args) {
        try {
            System.out.println("File Exchange Server is running...");

            ServerSocket serverSocket = new ServerSocket(12345);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getRemoteSocketAddress());

                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try {
            DataInputStream in = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());

            out.writeUTF("Server says: Connection to the File Exchange Server is successful!");

            String command;
            while ((command = in.readUTF()) != null) {
                if (command.startsWith("/leave")) {
                    out.writeUTF("Server says: Connection closed. Thank you!");
                    break;
                } else if (command.startsWith("/register")) {
                    registerHandle(command, out, clientSocket);
                } else if (command.startsWith("/?")) {
                    out.writeUTF("");
                } else {
                    if (getClientHandle(clientSocket) != "Unknown") {
                        if (command.startsWith("/store")) {
                            sendFile(command, out, clientSocket);
                        } else if (command.startsWith("/dir")) {
                            requestDirectory(out);
                        } else if (command.startsWith("/get")) {
                            fetchFile(command, out);
                        } else if (command.startsWith("/all")) {
                            sendMsgBroadcast(command, clientSocket);
                        } else if (command.startsWith("/msg")) {
                            sendMsgUnicast(command, clientSocket);
                        } else {
                            out.writeUTF("Error: Command not found.");
                        }
                    } else {
                        out.writeUTF("Error: Please register before using these commands.");
                    }
                }
            }

            out.writeUTF("Server says: Connection closed. Thank you!");
            System.out.println("Client disconnected: " + clientSocket.getRemoteSocketAddress());

            in.close();
            out.close();
            clientSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void registerHandle(String command, DataOutputStream out, Socket clientSocket) {
        String[] tokens = command.split("\\s+");

        if (tokens.length == 2) {
            String handle = tokens[1].trim();

            if (clientHandles.containsKey(handle)) {
                try {
                    out.writeUTF("Error: Registration failed. Handle or alias already exists.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                clientHandles.put(handle, clientSocket);
                try {
                    out.writeUTF("Welcome " + handle + "!");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            try {
                out.writeUTF("Error: Registration failed. Handle or alias cannot be empty.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String getClientHandle(Socket clientSocket) {
        for (Map.Entry<String, Socket> entry : clientHandles.entrySet()) {
            if (entry.getValue() == clientSocket) {
                return entry.getKey();
            }
        }
        return "Unknown";
    }

    private static void broadcast(String message) {
        for (Map.Entry<String, Socket> entry : clientHandles.entrySet()) {
            Socket clientSocket = entry.getValue();
            try {
                DataOutputStream clientOut = new DataOutputStream(clientSocket.getOutputStream());
                clientOut.writeUTF(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void sendMsgBroadcast(String command, Socket senderSocket) {
        String[] tokens = command.split("\\s+");
    
        if (tokens.length > 1) {
            String senderHandle = getClientHandle(senderSocket);
            String message = String.join(" ", tokens).substring(tokens[0].length() + 1);
            String broadcastMessage = String.format("[%s to All]: %s", senderHandle, message);
            broadcast(broadcastMessage);
        } else {
            // Handle the case when there is no message provided after "/all"
            System.out.println("Error: No message provided for broadcast.");
        }
    }

    private static void sendMsgUnicast(String command, Socket senderSocket) {
        String[] tokens = command.split("\\s+");
    
        if (tokens.length > 2) {
            String recipientHandle = tokens[1];
            String senderHandle = getClientHandle(senderSocket);
    
            // Check if the recipient exists in the clientHandles map
            if (clientHandles.containsKey(recipientHandle)) {
                Socket recipientSocket = clientHandles.get(recipientHandle);
    
                // Construct the private message
                String message = String.join(" ", tokens).substring(tokens[0].length() + recipientHandle.length() + 2);
                String senderMessage = String.format("[To %s]: %s", recipientHandle, message);
                String recipientMessage = String.format("[From %s]: %s", senderHandle, message);
    
                try {
                    // Send the message to the sender and recipient
                    DataOutputStream senderOut = new DataOutputStream(senderSocket.getOutputStream());
                    senderOut.writeUTF(senderMessage);
    
                    DataOutputStream recipientOut = new DataOutputStream(recipientSocket.getOutputStream());
                    recipientOut.writeUTF(recipientMessage);
    
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Error: Recipient not found.");
            }
        } else {
            // Handle the case when there is no recipient or message provided after "/msg"
            System.out.println("Error: Invalid /msg command. Usage: /msg <recipient> <message>");
        }
    }

    private static void sendFile(String command, DataOutputStream out, Socket clientSocket) {
        String[] tokens = command.split("\\s+");

        if (tokens.length == 2) {
            String filename = tokens[1];
            Path clientFilePath = Paths.get(CLIENT_DIRECTORY, filename);
            Path serverFilePath = Paths.get(SERVER_DIRECTORY, filename);

            if (Files.exists(clientFilePath)) {
                try {
                    byte[] fileContent = Files.readAllBytes(clientFilePath);
                    Files.write(serverFilePath, fileContent);

                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String timestamp = dateFormat.format(new Date());

                    String message = String.format("%s<%s>: Uploaded %s.", getClientHandle(clientSocket), timestamp, filename);
                    broadcast(message);

                } catch (IOException e) {
                    e.printStackTrace();
                    try {
                        out.writeUTF("Error: Unable to send the file.");
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
            } else {
                try {
                    out.writeUTF("Error: File not found on the client side.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void requestDirectory(DataOutputStream out) {
        File serverDirectory = new File(SERVER_DIRECTORY);
        File[] files = serverDirectory.listFiles();

        if (files != null) {
            String[] fileNames = new String[files.length];
            for (int i = 0; i < files.length; i++) {
                fileNames[i] = files[i].getName();
            }
            try {
                out.writeUTF("Server Directory: " + String.join(", ", fileNames));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                out.writeUTF("Server says: No files in the server directory.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void fetchFile(String command, DataOutputStream out) {
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
                    out.writeUTF("File received from Server: " + filename);
                } catch (IOException e) {
                    e.printStackTrace();
                    try {
                        out.writeUTF("Error: Unable to fetch or save the file.");
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
            } else {
                try {
                    out.writeUTF("Error: File not found on the server.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            try {
                out.writeUTF("Error: Command parameters do not match or are not allowed.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
