package scc.kube;

import java.util.UUID;
import java.util.logging.Logger;

import redis.clients.jedis.JedisPool;
import scc.Result;
import scc.ServiceError;
import scc.SessionToken;
import scc.kube.config.KubeConfig;
import scc.kube.dao.UserDao;

public class Auth {

    private static final Logger logger = Logger.getLogger(Auth.class.getName());

    private final KubeConfig config;
    private final JedisPool jedisPool;
    private final KubeData data;

    public Auth(KubeConfig config, JedisPool jedisPool, KubeData data) {
        this.config = config;
        this.jedisPool = jedisPool;
        this.data = data;
    }

    public Result<SessionToken, ServiceError> authenticate(String userId, String password) {
        logger.fine("Authenticating user " + userId);
        var userResult = this.data.getUser(userId);
        if (userResult.isError()) {
            logger.fine("Failed to get user: " + userResult.error());
            return Result.err(userResult.error());
        }

        logger.fine("User found, checking password");
        var userDao = userResult.value();
        if (userDao.getStatus() != UserDao.Status.ACTIVE) {
            logger.fine("User is not active");
            return Result.err(ServiceError.INVALID_CREDENTIALS);
        }

        if (!userDao.getHashedPassword().equals(Kube.hashUserPassword(password))) {
            logger.fine("Password incorrect");
            return Result.err(ServiceError.INVALID_CREDENTIALS);
        }

        logger.fine("Password correct, creating session token");
        var token = new SessionToken(UUID.randomUUID().toString());
        try (var jedis = this.jedisPool.getResource()) {
            Redis.setSession(jedis, userId, token.getToken());
        }

        return Result.ok(token);
    }

    public Result<String, ServiceError> validate(SessionToken token) {
        logger.fine("Validating session token " + token.getToken());
        try (var jedis = jedisPool.getResource()) {
            var userId = Redis.getSession(jedis, token.getToken());
            if (userId == null) {
                logger.fine("Session token not found");
                return Result.err(ServiceError.INVALID_CREDENTIALS);
            }

            logger.fine("Session token found, user id is " + userId);
            return Result.ok(userId);
        }
    }

    public Result<Void, ServiceError> match(SessionToken token, String userId) {
        logger.fine("Matching session token " + token.getToken() + " with user id " + userId);
        var result = this.validate(token);
        if (result.isError()) {
            logger.fine("Failed to validate session token: " + result.error());
            return Result.err(result.error());
        }

        if (!result.value().equals(userId)) {
            logger.fine("Session token does not match user id");
            return Result.err(ServiceError.INVALID_CREDENTIALS);
        }

        logger.fine("Session token matches user id");
        return Result.ok();
    }
}
