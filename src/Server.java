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
    private static final String SERVER_DIRECTORY = "ServerDirectory";
    private static final String CLIENT_DIRECTORY = "ClientDirectory";

    public static void main(String[] args) {
        try {
            System.out.println("\u001B[32mFile Exchange Server is running...\u001B[0m");

            ServerSocket serverSocket = new ServerSocket(12345);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("\u001B[32mClient connected: " + clientSocket.getRemoteSocketAddress() + "\u001B[0m");

                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            System.out.println("\u001B[31mError: " + e.getMessage() + "\u001B[0m");
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try {
            DataInputStream in = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());

            out.writeUTF("\u001B[32mServer says: Connection to the File Exchange Server is successful!\u001B[0m");

            String command;
            while ((command = in.readUTF()) != null) {
                if (command.startsWith("/leave")) {
                    out.writeUTF("\u001B[32mServer says: Connection closed. Thank you!\u001B[0m");
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
                            out.writeUTF("\u001B[31mError: Command not found.\u001B[0m");
                        }
                    } else {
                        out.writeUTF("\u001B[31mError: Please register before using these commands.\u001B[0m");
                    }
                }
            }

            System.out.println("\u001B[32mClient disconnected: " + clientSocket.getRemoteSocketAddress() + "\u001B[0m");

            in.close();
            out.close();
            clientSocket.close();

        } catch (IOException e) {
            System.out.println("\u001B[31mError: " + e.getMessage() + "\u001B[0m");
            e.printStackTrace();
        }
    }

    private static void registerHandle(String command, DataOutputStream out, Socket clientSocket) {
        String[] tokens = command.split("\\s+");

        if (tokens.length == 2) {
            String handle = tokens[1].trim();

            if (clientHandles.containsKey(handle)) {
                try {
                    out.writeUTF("\u001B[31mError: Registration failed. Handle or alias already exists.\u001B[0m");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                clientHandles.put(handle, clientSocket);
                try {
                    out.writeUTF("\u001B[32mWelcome " + handle + "!\u001B[0m");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            try {
                out.writeUTF("\u001B[31mError: Registration failed. Handle or alias cannot be empty.\u001B[0m");
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

                    String message = String.format("\u001B[32m%s<%s>: Uploaded %s.\u001B[0m", getClientHandle(clientSocket), timestamp, filename);
                    broadcastMessage(message);

                } catch (IOException e) {
                    e.printStackTrace();
                    try {
                        out.writeUTF("\u001B[31mError: Unable to send the file.\u001B[0m");
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
            } else {
                try {
                    out.writeUTF("\u001B[31mError: File not found on the client side.\u001B[0m");
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
                out.writeUTF("\u001B[32mServer Directory: " + String.join(", ", fileNames) + "\u001B[0m");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                out.writeUTF("\u001B[32mServer says: No files in the server directory.\u001B[0m");
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
                    out.writeUTF("\u001B[32mFile received from Server: " + filename + "\u001B[0m");
                } catch (IOException e) {
                    e.printStackTrace();
                    try {
                        out.writeUTF("\u001B[31mError: Unable to fetch or save the file.\u001B[0m");
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
            } else {
                try {
                    out.writeUTF("\u001B[31mError: File not found on the server.\u001B[0m");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            try {
                out.writeUTF("\u001B[31mError: Command parameters do not match or are not allowed.\u001B[0m");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
