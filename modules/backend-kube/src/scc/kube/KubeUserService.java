package scc.kube;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import redis.clients.jedis.JedisPool;
import scc.Result;
import scc.ServiceError;
import scc.SessionToken;
import scc.UpdateUserOps;
import scc.UserService;
import scc.item.UserItem;
import scc.kube.config.KubeConfig;
import scc.kube.dao.UserDao;

public class KubeUserService implements UserService {

    private final KubeConfig config;
    private final JedisPool jedisPool;
    private final Mongo mongo;
    private final Rabbitmq rabbitmq;

    public KubeUserService(KubeConfig config, JedisPool jedisPool, Mongo mongo, Rabbitmq rabbitmq) {
        this.config = config;
        this.jedisPool = jedisPool;
        this.mongo = mongo;
        this.rabbitmq = rabbitmq;
    }

    @Override
    public Result<UserItem, ServiceError> createUser(CreateUserParams params) {
        if (params.id().isBlank() || params.name().isBlank() || params.password().isBlank())
            return Result.err(ServiceError.BAD_REQUEST);

        try (var jedis = this.jedisPool.getResource()) {
            var data = new KubeData(this.config, this.mongo, jedis);

            var userDao = new UserDao();
            userDao.username = params.id();
            userDao.name = params.name();
            userDao.hashedPassword = Kube.hashUserPassword(params.password());
            userDao.status = UserDao.Status.ACTIVE;
            userDao.createTime = LocalDateTime.now(ZoneOffset.UTC);

            var result = data.createUser(userDao);
            if (result.isError())
                return Result.err(result);

            userDao = result.value();
            var userItem = data.userDaoToItem(userDao);

            return Result.ok(userItem);
        }
    }

    @Override
    public Result<UserItem, ServiceError> getUser(String username) {
        if (username.isBlank())
            return Result.err(ServiceError.BAD_REQUEST);

        try (var jedis = this.jedisPool.getResource()) {
            var data = new KubeData(this.config, this.mongo, jedis);
            var result = data.getUserByUsername(username);
            if (result.isError())
                return Result.err(result);
            var userDao = result.value();
            var userItem = data.userDaoToItem(userDao);
            return Result.ok(userItem);
        }
    }

    @Override
    public Result<UserItem, ServiceError> deleteUser(SessionToken token, String userName) {
        if (userName.isBlank())
            return Result.err(ServiceError.BAD_REQUEST);

        try (var jedis = this.jedisPool.getResource()) {
            var data = new KubeData(this.config, this.mongo, jedis);
            var authResult = data.validate(token);
            if (authResult.isError())
                return Result.err(authResult);
            var userId = authResult.value();

            var result = data.deactivateUser(userId);
            if (result.isError())
                return Result.err(result);

            this.rabbitmq.deleteUser(userId.toHexString());

            var userDao = result.value();
            var userItem = data.userDaoToItem(userDao);
            return Result.ok(userItem);
        }
    }

    @Override
    public Result<UserItem, ServiceError> updateUser(SessionToken token, String userName, UpdateUserOps ops) {
        if (userName.isBlank())
            return Result.err(ServiceError.BAD_REQUEST);

        try (var jedis = this.jedisPool.getResource()) {
            var data = new KubeData(this.config, this.mongo, jedis);
            var authResult = data.validate(token);
            if (authResult.isError())
                return Result.err(authResult);
            var userId = authResult.value();

            var userDao = new UserDao();
            if (ops.shouldUpdateName())
                userDao.name = ops.getName();
            if (ops.shouldUpdatePassword())
                userDao.hashedPassword = Kube.hashUserPassword(ops.getPassword());
            if (ops.shouldUpdateImage())
                userDao.profileImageId = Kube.mediaIdToString(ops.getImageId());

            var updateResult = data.updateUser(userId, userDao);
            if (updateResult.isError())
                return Result.err(updateResult);

            userDao = updateResult.value();
            var userItem = data.userDaoToItem(userDao);
            return Result.ok(userItem);
        }
    }

    @Override
    public Result<SessionToken, ServiceError> authenticateUser(String userName, String password) {
        try (var jedis = this.jedisPool.getResource()) {
            var data = new KubeData(this.config, this.mongo, jedis);
            return data.authenticate(userName, password);
        }
    }

}
