package scc.cache;

import java.util.Optional;

public interface Cache {

    /**
     * Returns the JSON string from cache with given key
     * @param key key for the value stored in cache
     * @return JSON object
     */
    String get(String key);

    /**
     * Returns the byte contents of a media resource with given key
     * @param key key for the value stored in cache
     * @return bytes of the media resource
     */
    Optional<byte[]> getBytes(String key);

    /**
     * Sets the value (JSON object) into a given key
     * @param key key of the value
     * @param value value to be stored in cache
     * @return Result of the operation
     */
    String set(String key, String value);

    /**
     * Sets the bytes into a given key
     * @param key key of the value
     * @param value byte content to be stored in cache
     * @return Result of the operation
     */
    String setBytes(String key, byte[] value);

    /**
     * Sets the expiration time for a given key
     * @param key key of the value stored in cache
     * @param seconds time to live in cache
     * @return Result of the operation
     */
    Long expire(String key, int seconds);

    /**
     * Deletes the entries with given keys from the cache
     * @param keys keys of values stored in cache
     * @return Result of the operation
     */
    Long del(String... keys);
}
