package scc.azure;

import java.util.UUID;
import java.util.logging.Logger;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;

import redis.clients.jedis.JedisPool;
import scc.Result;
import scc.ServiceError;
import scc.SessionToken;
import scc.azure.config.AzureConfig;
import scc.azure.dao.UserDAO;

public class Auth {
    private static final Logger logger = Logger.getLogger(Auth.class.getName());

    private final AzureConfig config;
    private final JedisPool jedisPool;
    private final CosmosContainer userContainer;

    public Auth(AzureConfig config, JedisPool jedisPool, CosmosDatabase database) {
        this.config = config;
        this.jedisPool = jedisPool;
        this.userContainer = database.getContainer(config.getCosmosDbConfig().userContainer);
    }

    public Result<SessionToken, ServiceError> authenticate(String userId, String password) {
        logger.fine("Authenticating user " + userId);
        var userResult = AzureData.getUser(this.config, this.jedisPool, this.userContainer, userId);
        if (userResult.isError()) {
            logger.fine("Failed to get user: " + userResult.error());
            return Result.err(userResult.error());
        }

        logger.fine("User found, checking password");
        var userDao = userResult.value();
        if (userDao.getStatus() != UserDAO.Status.ACTIVE) {
            logger.fine("User is not active");
            return Result.err(ServiceError.INVALID_CREDENTIALS);
        }

        if (!userDao.getHashedPwd().equals(Azure.hashUserPassword(password))) {
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
