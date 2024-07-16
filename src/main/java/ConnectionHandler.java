import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * The ConnectionHandler class handles a client connection, processes commands, and communicates with the client
 * through the InputHandler and OutputHandler. It also supports replication for a master-slave architecture.
 */
public class ConnectionHandler implements Runnable {
    private final Socket socket;
    private final InputHandler in;
    private final OutputHandler out;
    private final KeyValueHandler store;
    private boolean isReplicationConnection;
    private static List<BlockingQueue<String[]>> queues = new ArrayList<>();
    private static long master_repl_offset = 0;
    private static List<Long> replOffsets = new ArrayList<>();

    /**
     * Constructs a ConnectionHandler with the specified socket and key-value store.
     *
     * @param socket the client socket
     * @param store  the key-value store
     * @throws IOException if an I/O error occurs
     */
    public ConnectionHandler(Socket socket, KeyValueHandler store) throws IOException {
        this.socket = socket;
        this.in = new InputHandler(socket.getInputStream());
        this.out = new OutputHandler(socket.getOutputStream());
        this.store = store;
    }

    /**
     * Constructs a ConnectionHandler with the specified socket, key-value store, and replication port.
     * Sets up the connection for replication by sending necessary commands to the master.
     *
     * @param socket the client socket
     * @param store  the key-value store
     * @param port   the replication port
     * @throws IOException if an I/O error occurs
     */
    public ConnectionHandler(Socket socket, KeyValueHandler store, int port) throws IOException {
        this(socket, store);
        this.isReplicationConnection = true;
        // PING
        out.writeBulkStringArray("PING");
        in.readSimpleString();
        // REPLCONF listening-port <PORT>
        out.writeBulkStringArray("REPLCONF", "listening-port", String.valueOf(port));
        in.readSimpleString();
        // REPLCONF capa psync2
        out.writeBulkStringArray("REPLCONF", "capa", "psync2");
        in.readSimpleString();
        // PSYNC ? -1
        out.writeBulkStringArray("PSYNC", "?", "-1");
        in.readSimpleString();
        int contentLength = Integer.parseInt(in.readLine().substring(1));
        in.readNBytes(contentLength);
        in.resetPosition();
    }

    /**
     * The main run method for the thread. Processes commands from the client and handles them appropriately.
     */
    @Override
    public void run() {
        System.out.println("this is a: " + Main.role);
        try (socket; in; out) {
            while (true) {
                long position = in.getPosition();
                String[] args = in.readBulkStringArray();
                String commandName = args[0].toUpperCase();
                switch (commandName) {
                    case "PING" -> handlePingCommand();
                    case "ECHO" -> handleEchoCommand(args);
                    case "SET" -> handleSetCommand(args);
                    case "GET" -> handleGetCommand(args);
                    case "INFO" -> handleInfoCommand();
                    case "REPLCONF" -> handleReplconfCommand(args, position);
                    case "PSYNC" -> handlePsyncCommand();
                    case "WAIT" -> handleWaitCommand(args);
                    case "CONFIG" -> handleConfigCommand();
                }
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("Exception: " + e.getMessage());
        }
    }

    /**
     * Handles the PING command from the client. Responds with PONG if not a replication connection.
     *
     * @throws IOException if an I/O error occurs
     */
    private void handlePingCommand() throws IOException {
        if (!isReplicationConnection) {
            out.writeSimpleString("PONG");
        }
    }

    /**
     * Handles the ECHO command from the client. Responds with the same message.
     *
     * @param args the command arguments
     * @throws IOException          if an I/O error occurs
     * @throws InterruptedException if interrupted while waiting
     */
    private void handleEchoCommand(String[] args) throws IOException, InterruptedException {
        String message = args[1];
        out.writeBulkString(message);
    }

    /**
     * Handles the SET command from the client. Sets a key-value pair in the store and optionally sets an expiration.
     *
     * @param args the command arguments
     * @throws IOException          if an I/O error occurs
     * @throws InterruptedException if interrupted while waiting
     */
    private void handleSetCommand(String[] args) throws IOException, InterruptedException {
        String key = args[1];
        String value = args[2];
        if (args.length == 5 && args[3].equals("px")) {
            int milliseconds = Integer.parseInt(args[4]);
            store.set(key, value, milliseconds);
        } else {
            store.set(key, value);
        }
        if (Main.role.equals("master")) {
            propagateToReplicas(args);
            out.writeSimpleString("OK");
        }
    }

    /**
     * Handles the GET command from the client. Retrieves a value from the store.
     *
     * @param args the command arguments
     * @throws IOException if an I/O error occurs
     */
    private void handleGetCommand(String[] args) throws IOException {
        String key = args[1];
        String value = store.get(key);
        System.out.println("t=" + Thread.currentThread().getName() + ", get key=" + key + ", value=" + value);
        if (value == null) {
            out.writeNullBulkString();
        } else {
            out.writeBulkString(value);
        }
    }

    /**
     * Handles the INFO command from the client. Responds with server information.
     *
     * @throws IOException if an I/O error occurs
     */
    private void handleInfoCommand() throws IOException {
        Map<String, String> infoFields = new HashMap<>();
        infoFields.put("role", Main.role);
        if (Main.role.equals("master")) {
            infoFields.put("master_replid", Main.master_replid);
            infoFields.put("master_repl_offset", "" + master_repl_offset);
        }
        out.writeBulkString(infoFields);
    }

    /**
     * Handles the REPLCONF command from the client. Responds with acknowledgment if needed.
     *
     * @param args     the command arguments
     * @param position the current read position
     * @throws IOException if an I/O error occurs
     */
    private void handleReplconfCommand(String[] args, long position) throws IOException {
        if (args[1].equals("GETACK")) {
            String[] response = {"REPLCONF", "ACK", String.valueOf(position)};
            out.writeBulkStringArray(response);
        } else {
            out.writeSimpleString("OK");
        }
    }

    /**
     * Handles the PSYNC command from the client. Initiates full resynchronization.
     *
     * @throws IOException          if an I/O error occurs
     * @throws InterruptedException if interrupted while waiting
     */
    private void handlePsyncCommand() throws IOException, InterruptedException {
        byte[] contents = HexFormat.of().parseHex("524544495330303131fa0972656469732d76657205372e322e30fa0a72656469732d62697473c040fa056374696d65c26d08bc65fa08757365642d6d656dc2b0c41000fa08616f662d62617365c000fff06e3bfec0ff5aa2");
        out.writeSimpleString("FULLRESYNC %s 0".formatted(Main.master_replid));
        out.writeBulkString(contents);
        propagateToReplica();
    }

    /**
     * Handles the WAIT command from the client. Waits for replication to complete to a specified number of replicas.
     *
     * @param args the command arguments
     * @throws IOException          if an I/O error occurs
     * @throws InterruptedException if interrupted while waiting
     */
    private void handleWaitCommand(String[] args) throws IOException, InterruptedException {
        int expectedReplicas = Integer.parseInt(args[1]);
        int actualReplicas = 0;
        int connectedReplicas = queues.size();
        long timeoutTimestamp = System.currentTimeMillis() + Long.parseLong(args[2]);
        long currentReplicationOffset = master_repl_offset;
        System.out.println("connected replica=" + connectedReplicas);
        if (currentReplicationOffset == 0) {
            out.writeInteger(connectedReplicas);
            return;
        }
        replOffsets.clear();
        propagateToReplicas(new String[]{"REPLCONF", "GETACK", "*"});
        while (true) {
            if (actualReplicas >= expectedReplicas) {
                break;
            }
            if (System.currentTimeMillis() > timeoutTimestamp) {
                break;
            }
            actualReplicas = 0;
            System.out.println("new iteration");
            System.out.println("   expected=" + expectedReplicas);
            System.out.println("   currentReplicationOffset=" + currentReplicationOffset);
            for (long replicaOffset : replOffsets) {
                System.out.println("   replicaOffset=" + replicaOffset);
                if (replicaOffset >= currentReplicationOffset) {
                    actualReplicas++;
                }
            }
            Thread.sleep(200);
        }
        System.out.println("wait response actual=" + actualReplicas);
        out.writeInteger(actualReplicas);
    }

    /**
     * Handles the CONFIG command from the client.
     */
    private void handleConfigCommand() {

    }

    /**
     * Propagates a command to all replicas.
     *
     * @param args the command arguments
     * @throws InterruptedException if interrupted while waiting
     */
    private void propagateToReplicas(String[] args) throws InterruptedException {
        master_repl_offset += out.getBulkStringArray(args).length;
        for (BlockingQueue<String[]> queue : queues) {
            queue.put(args);
        }
    }

    /**
     * Propagates commands to a single replica.
     *
     * @throws InterruptedException if interrupted while waiting
     * @throws IOException          if an I/O error occurs
     */
    private void propagateToReplica() throws InterruptedException, IOException {
        BlockingQueue<String[]> queue = new LinkedBlockingDeque<>();
        queues.add(queue);
        Thread.ofVirtual().start(() -> {
            while (true) {
                try {
                    String[] response = in.readBulkStringArray();
                    replOffsets.add(Long.parseLong(response[2]));
                } catch (IOException e) {
                    new RuntimeException(e);
                }
            }
        });
        while (true) {
            String[] args = queue.take();
            System.out.println("propagating: " + Arrays.toString(args));
            out.writeBulkStringArray(args);
        }
    }

}
