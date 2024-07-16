import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

/**
 * The Main class sets up and runs a Redis-like server. It handles client connections
 * and supports both master and slave roles for replication.
 */
public class Main {
    public static int port = 6379;
    public static String role = "master";
    // replica-only
    public static String master_host;
    public static int master_port;
    // master-only
    public static String master_replid = "8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb";

    /**
     * The main entry point for the application. Sets up the server and handles
     * incoming connections.
     *
     * @param args the command-line arguments
     * @throws IOException if an I/O error occurs
     */
    public static void main(String[] args) throws IOException {
        KeyValueHandler store = new KeyValueHandler();

        // Parse command-line arguments for port and replication settings
        if (args.length >= 2 && args[0].equalsIgnoreCase("--port")) {
            port = Integer.parseInt(args[1]);
        }

        if (args.length >= 4 && args[2].equalsIgnoreCase("--replicaof")) {
            role = "slave";
            master_host = args[3].split(" ")[0];
            master_port = Integer.parseInt(args[3].split(" ")[1]);
            Socket socket = new Socket(master_host, master_port);
            Thread.ofVirtual().start(new ConnectionHandler(socket, store, port));
        }
//-------------------------------------------------DEBUGGING----------------------------------------------------------//
//        System.out.println("This here is a: " + role);                                                              //
//        System.out.println(Arrays.toString(args));                                                                  //
//--------------------------------------------------------------------------------------------------------------------//
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            // Setting SO_REUSEADDR ensures that we don't run into 'Address already in use' errors
            serverSocket.setReuseAddress(true);
            while (true) {
                Socket socket = serverSocket.accept();
                Thread.ofVirtual().start(new ConnectionHandler(socket, store));
            }
        }
    }
}
