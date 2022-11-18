package scc.azure;

import java.util.UUID;

import redis.clients.jedis.JedisPool;
import scc.Result;
import scc.ServiceError;
import scc.SessionToken;
import scc.azure.repo.UserRepo;

public class Auth {
    private final UserRepo repo;
    private final JedisPool pool;

    public Auth(UserRepo repo, JedisPool jedisPool) {
        this.repo = repo;
        this.pool = jedisPool;
    }

    public Result<SessionToken, ServiceError> authenticate(String userId, String password) {
        var userResult = repo.getUser(userId);
        if (userResult.isError())
            return Result.err(userResult.error());
        var user = userResult.value();
        if (!user.getHashedPwd().equals(Azure.hashUserPassword(password)))
            return Result.err(ServiceError.INVALID_CREDENTIALS);
        var token = new SessionToken(UUID.randomUUID().toString());
        try (var jedis = pool.getResource()) {
            Redis.setSession(jedis, userId, token.getToken());
        }
        return Result.ok(token);
    }

    public Result<String, ServiceError> validate(SessionToken token) {
        try (var jedis = pool.getResource()) {
            var userId = Redis.getSession(jedis, token.getToken());
            if (userId == null)
                return Result.err(ServiceError.INVALID_CREDENTIALS);
            return Result.ok(userId);
        }
    }
}
