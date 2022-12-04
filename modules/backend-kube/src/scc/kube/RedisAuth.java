package scc.kube;

import java.util.UUID;
import java.util.logging.Logger;

import redis.clients.jedis.Jedis;
import scc.SessionToken;
import scc.exception.BadCredentialsException;
import scc.exception.UnauthorizedException;
import scc.exception.UserNotFoundException;

public class RedisAuth implements Auth {
    private static final Logger logger = Logger.getLogger(RedisAuth.class.getName());

    private final Jedis jedis;
    private final Mongo mongo;

    public RedisAuth(Jedis jedis, Mongo mongo) {
        this.jedis = jedis;
        this.mongo = mongo;
    }

    @Override
    public SessionToken authenticate(String username, String password)
            throws BadCredentialsException, UserNotFoundException {
        var userDao = this.mongo.getUserByUsername(username);
        if (!userDao.hashedPassword.equals(Kube.hashUserPassword(password)))
            throw new BadCredentialsException();
        var token = new SessionToken(UUID.randomUUID().toString());
        Redis.setSession(this.jedis, username, token.getToken());
        return token;
    }

    @Override
    public String validate(SessionToken token) throws BadCredentialsException {
        logger.fine("Validating token: " + token.getToken());
        var username = Redis.getSession(this.jedis, token.getToken());
        if (username == null) {
            logger.fine("Token not found");
            throw new BadCredentialsException("Token not found");
        }
        logger.fine("Token found, userId: " + username);
        return username;
    }

    @Override
    public String validate(SessionToken token, String username) throws BadCredentialsException, UnauthorizedException {
        var userId = this.validate(token);
        if (!userId.equals(username))
            throw new UnauthorizedException("Token does not belong to user");
        return userId;
    }

    @Override
    public void close() throws Exception {
    }

}
