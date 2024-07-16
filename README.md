# Jedis-like Server

## Overview

This project is a simplified implementation of a Redis-like server, written in Java. It supports basic key-value storage, expiration of keys, and master-slave replication. The server is designed to handle multiple client connections concurrently using Java's `Thread` class.

## Current Features

- **Key-Value Storage**: Store and retrieve string values associated with string keys.
- **Expiration**: Set a key-value pair with an expiration time.
- **Master-Slave Replication**: Supports replication for a master-slave architecture.
- **Concurrent Clients**: Handles multiple clients concurrently.
- **Basic Redis Commands**: Implements basic Redis commands such as PING, ECHO, SET, GET, INFO, REPLCONF, PSYNC, and WAIT.

## Future Features (Soon to be available in Decending Order)
### Future Features

#### RDB Persistence

**RDB (Redis Database) Persistence**:
- **Description**: This feature will enable the server to persist data on disk, allowing it to save the state of the key-value store at specified intervals. This ensures data durability, meaning the data will survive a server restart.
- **Benefit**: Ensures data is not lost between server restarts and provides a way to backup and restore the database.

#### Transactions

**Transactions**:
- **Description**: This feature will introduce the ability to group multiple commands into a single transaction. All commands within a transaction will be executed in a sequence without interruption, ensuring atomicity.
- **Benefit**: Provides a way to execute multiple commands in a safe and predictable manner, ensuring that either all commands in the transaction are executed, or none are, maintaining data consistency.

#### Streams

**Streams**:
- **Description**: This feature will add support for handling streams, allowing the server to process and store data that arrives continuously. It will enable use cases like message queues and real-time data processing.
- **Benefit**: Allows the server to efficiently handle real-time data feeds, making it suitable for applications requiring continuous data ingestion and processing, such as chat applications, IoT data collection, and event logging.

## Getting Started

### Prerequisites

- Java Development Kit (JDK) 11 or later

### Running the Server

1. **Compile the code:**

   ```sh
   javac *.java
   ```

2. **Run the server:**

   ```sh
   java Main [--port <port>] [--replicaof <host> <port>]
   ```

   - `--port <port>`: (Optional) Specify the port on which the server will listen (default: 6379).
   - `--replicaof <host> <port>`: (Optional) Run the server as a slave, replicating from the specified master.

### Example Usage

1. **Start the master server:**

   ```sh
   java Main --port 6379
   ```

2. **Start the slave server:**

   ```sh
   java Main --port 6380 --replicaof localhost 6379
   ```

### Connecting to the Server

You can use any Redis client to connect to the server, such as `redis-cli`:

```sh
redis-cli -p 6379
```

## Classes and Their Responsibilities

### `Main`

- Entry point for the application.
- Sets up the server and handles incoming connections.
- Supports both master and slave roles for replication.

### `ConnectionHandler`

- Handles a client connection, processes commands, and communicates with the client.
- Supports replication for a master-slave architecture.
- Implements various Redis commands such as PING, ECHO, SET, GET, INFO, REPLCONF, PSYNC, and WAIT.

### `InputHandler`

- Provides methods to read and process data from an `InputStream`.
- Reads bytes, simple strings, lines, and bulk string arrays.
- Keeps track of the read position within the stream.

### `OutputHandler`

- Provides methods to write different types of responses to an `OutputStream`.
- Supports writing integers, simple strings, bulk strings, null bulk strings, maps, and arrays of strings.

### `KeyValueHandler`

- Provides a thread-safe in-memory key-value store with optional time-based expiration.
- Sets, gets, and deletes key-value pairs.
- Manages expiration times for keys.

## Command Implementations

### `PING`

- Responds with `PONG`.

### `ECHO <message>`

- Responds with the same message.

### `SET <key> <value> [px <milliseconds>]`

- Sets a key-value pair in the store.
- Optionally sets an expiration time.

### `GET <key>`

- Retrieves the value associated with the specified key.

### `INFO`

- Responds with server information.

### `REPLCONF`

- Handles replication configuration commands.

### `PSYNC`

- Initiates partial synchronization with a master.

### `WAIT <numreplicas> <timeout>`

- Waits for replication to complete to a specified number of replicas.

## Contributing

Contributions are welcome! Please open an issue or submit a pull request.


## Contact

For any questions or feedback, please contact (https://github.com/Italo115).

