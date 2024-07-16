import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The KeyValueHandler class provides a thread-safe in-memory key-value store with optional time-based expiration.
 */
public class KeyValueHandler {
    // Concurrent map to store key-value pairs
    private Map<String, String> map = new ConcurrentHashMap<>();
    // Concurrent map to store expiration times for keys
    private Map<String, Long> expiry = new ConcurrentHashMap<>();

    /**
     * Sets a key-value pair in the store.
     *
     * @param key   the key to be set
     * @param value the value to be associated with the key
     */
    public synchronized void set(String key, String value) {
        map.put(key, value);
        expiry.remove(key);
    }

    /**
     * Sets a key-value pair in the store with an expiration time.
     *
     * @param key          the key to be set
     * @param value        the value to be associated with the key
     * @param milliseconds the time in milliseconds after which the key-value pair should expire
     */
    public synchronized void set(String key, String value, int milliseconds) {
        map.put(key, value);
        expiry.put(key, System.currentTimeMillis() + milliseconds);
    }

    /**
     * Retrieves the value associated with the specified key.
     * If the key has expired, it will be removed from the store and null will be returned.
     *
     * @param key the key whose associated value is to be returned
     * @return the value associated with the specified key, or null if the key does not exist or has expired
     */
    public synchronized String get(String key) {
        deleteIfExpired(key);
        return map.get(key);
    }

    /**
     * Checks if a key has expired and removes it from the store if it has.
     * This method is called internally by the get method to ensure expired keys are cleaned up.
     *
     * @param key the key to check for expiration
     */
    private void deleteIfExpired(String key) {
        Long expiryTime = expiry.get(key);
        if (expiryTime != null && System.currentTimeMillis() > expiryTime) {
            map.remove(key);
            expiry.remove(key);
        }
    }
}
