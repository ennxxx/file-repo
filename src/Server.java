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

    private static void broadcastMessage(String message) {
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
                    broadcastMessage(message);

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
