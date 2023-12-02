import java.net.Socket;

public class FileClient {
    public static void main(String[] args) {
        try {

            // Establish connection with the server
            System.out.println("Client started");
            Socket soc = new Socket("localhost", 9806);

        } catch(Exception e) {
            e.printStackTrace();
        }

    }
}
