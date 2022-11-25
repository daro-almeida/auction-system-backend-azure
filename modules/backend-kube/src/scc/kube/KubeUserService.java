package scc.kube;

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

    private final Mongo mongo;
    private final KubeData data;
    private final Auth auth;

    public KubeUserService(KubeConfig config, JedisPool jedisPool, Mongo mongo) {
        this.mongo = mongo;
        this.data = new KubeData(config, mongo, jedisPool);
        this.auth = new Auth(config, jedisPool, this.data);
    }

    @Override
    public Result<UserItem, ServiceError> createUser(CreateUserParams params) {
        if (params.id().isBlank() || params.name().isBlank() || params.password().isBlank())
            return Result.err(ServiceError.BAD_REQUEST);

        var userDao = new UserDao(
                params.id(),
                params.name(),
                Kube.hashUserPassword(params.password()),
                null,
                UserDao.Status.ACTIVE);

        var createResult = this.mongo.createUser(userDao);
        if (createResult.isError())
            return Result.err(createResult);

        this.data.setUser(userDao);
        var userItem = this.data.userDaoToItem(userDao);

        return Result.ok(userItem);
    }

    @Override
    public Result<UserItem, ServiceError> getUser(String userId) {
        if (userId.isBlank())
            return Result.err(ServiceError.BAD_REQUEST);

        var userResult = this.data.getUser(userId);
        if (userResult.isError())
            return Result.err(userResult);

        var userDao = userResult.value();
        var userItem = this.data.userDaoToItem(userDao);

        return Result.ok(userItem);
    }

    @Override
    public Result<UserItem, ServiceError> deleteUser(SessionToken token, String userId) {
        var result = this.data.getUser(userId);
        if (result.isError())
            return Result.err(result);

        var userDao = result.value();
        var userItem = this.data.userDaoToItem(userDao);
        // TODO: message queue

        return Result.ok(userItem);
    }

    @Override
    public Result<UserItem, ServiceError> updateUser(SessionToken token, String userId, UpdateUserOps ops) {
        var authResult = this.auth.match(token, userId);
        if (authResult.isError())
            return Result.err(authResult);

        var userDao = new UserDao();
        if (ops.shouldUpdateName())
            userDao.setName(ops.getName());
        if (ops.shouldUpdatePassword())
            userDao.setHashedPassword(Kube.hashUserPassword(ops.getPassword()));
        // if (ops.shouldUpdateImage()) // TODO: fix this
        // userDao.setPhotoId(K.mediaIdToString(ops.getImageId()));

        var updateResult = this.mongo.updateUser(userDao);
        if (updateResult.isError())
            return Result.err(updateResult);

        userDao = updateResult.value();
        this.data.setUser(userDao);
        var userItem = this.data.userDaoToItem(userDao);

        return Result.ok(userItem);
    }

    @Override
    public Result<SessionToken, ServiceError> authenticateUser(String userId, String password) {
        if (userId.isBlank() || password.isBlank())
            return Result.err(ServiceError.BAD_REQUEST);
        return this.auth.authenticate(userId, password);
    }

}
