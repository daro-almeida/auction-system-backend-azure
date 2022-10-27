package scc.azure;

import java.util.UUID;

import redis.clients.jedis.JedisPool;
import scc.services.ServiceError;
import scc.utils.Result;

public class UserAuth {
    private static final int SESSION_EXPIRE_SECONDS = 30 * 60;

    private final UserDB userDB;
    private final JedisPool jedisPool;

    public UserAuth(UserDB userDB, JedisPool jedisPool) {
        this.userDB = userDB;
        this.jedisPool = jedisPool;
    }

    /**
     * Checks if the provided user crenditals are valid.
     * If they are, a session token is generated and returned.
     * 
     * @param userId   Identifier of the user
     * @param password Password of the user
     * @return SessionToken or Error
     */
    public Result<String, ServiceError> authenticate(String userId, String password) {
        var userOpt = this.userDB.getUser(userId);
        if (userOpt.isEmpty())
            return Result.err(ServiceError.USER_NOT_FOUND);

        var user = userOpt.get();
        var hashedPassword = AzureUtils.hashUserPassword(password);
        if (!user.getHashedPwd().equals(hashedPassword))
            return Result.err(ServiceError.INVALID_CREDENTIALS);

        var sessionToken = this.generateSessionToken();
        var cacheKey = this.cacheKeyFromToken(sessionToken);
        var cacheValue = userId;

        try (var client = this.jedisPool.getResource()) {
            client.set(cacheKey, cacheValue);
            client.expire(cacheKey, SESSION_EXPIRE_SECONDS);
        }

        return Result.ok(sessionToken);
    }

    /**
     * Checks if the provided session token is valid.
     * If the token is valid, return the user identifier associated to it.
     * 
     * @param sessionToken
     * @return User identifier or Error
     */
    public Result<String, ServiceError> validateSessionToken(String sessionToken) {
        var cacheKey = this.cacheKeyFromToken(sessionToken);
        try (var client = this.jedisPool.getResource()) {
            var userId = client.get(cacheKey);
            if (userId == null)
                return Result.err(ServiceError.INVALID_CREDENTIALS);

            client.expire(cacheKey, SESSION_EXPIRE_SECONDS);
            return Result.ok(userId);
        }
    }

    /**
     * Deletes the entry from the cache associated to the given session token.
     * @param sessionToken Session token from the logged in user
     */
    public void deleteSessionToken(String sessionToken) {
        var cacheKey = this.cacheKeyFromToken(sessionToken);
        try (var client = this.jedisPool.getResource()){
            client.del(cacheKey);
        }
    }

    private String cacheKeyFromToken(String sessionToken) {
        return "session:" + sessionToken;
    }

    private String generateSessionToken() {
        return UUID.randomUUID().toString();
    }
}
