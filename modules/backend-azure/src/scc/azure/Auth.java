package scc.azure;

import java.util.UUID;
import java.util.logging.Logger;

import redis.clients.jedis.JedisPool;
import scc.Result;
import scc.ServiceError;
import scc.SessionToken;
import scc.azure.repo.UserRepo;

public class Auth {
    private static final Logger logger = Logger.getLogger(Auth.class.getName());

    private final UserRepo repo;
    private final JedisPool pool;

    public Auth(UserRepo repo, JedisPool jedisPool) {
        this.repo = repo;
        this.pool = jedisPool;
    }

    public Result<SessionToken, ServiceError> authenticate(String userId, String password) {
        logger.fine("Authenticating user " + userId);
        var userResult = repo.getUser(userId);
        if (userResult.isError()) {
            logger.fine("Failed to get user: " + userResult.error());
            return Result.err(userResult.error());
        }

        logger.fine("User found, checking password");
        var user = userResult.value();
        if (!user.getHashedPwd().equals(Azure.hashUserPassword(password))){
            logger.fine("Password incorrect");
            return Result.err(ServiceError.INVALID_CREDENTIALS);
        }

        logger.fine("Password correct, creating session token");
        var token = new SessionToken(UUID.randomUUID().toString());
        try (var jedis = pool.getResource()) {
            Redis.setSession(jedis, userId, token.getToken());
        }
        return Result.ok(token);
    }

    public Result<String, ServiceError> validate(SessionToken token) {
        logger.fine("Validating session token " + token.getToken());
        try (var jedis = pool.getResource()) {
            var userId = Redis.getSession(jedis, token.getToken());
            if (userId == null) {
                logger.fine("Session token not found");
                return Result.err(ServiceError.INVALID_CREDENTIALS);
            }

            logger.fine("Session token found, user id is " + userId);
            return Result.ok(userId);
        }
    }
}
