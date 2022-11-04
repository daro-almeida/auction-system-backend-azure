package scc.cache;

public interface UserCache {

    String USER_PREFIX = "user:";

    /**
     * Deletes a user entry with given key from the cache
     * @param userId identifier of the user
«    */
    void deleteUser(String userId);

    void set(String userId, String userJson);
}
