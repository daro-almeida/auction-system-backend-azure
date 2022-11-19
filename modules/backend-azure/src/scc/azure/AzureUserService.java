package scc.azure;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;

import redis.clients.jedis.JedisPool;
import scc.Result;
import scc.ServiceError;
import scc.SessionToken;
import scc.UpdateUserOps;
import scc.UserService;
import scc.azure.config.AzureConfig;
import scc.azure.dao.UserDAO;
import scc.item.UserItem;

public class AzureUserService implements UserService {
    private final AzureConfig azureConfig;
    private final JedisPool jedisPool;
    private final CosmosContainer userContainer;
    private final Auth2 auth;

    public AzureUserService(AzureConfig config, JedisPool jedisPool, CosmosDatabase database) {
        this.azureConfig = config;
        this.jedisPool = jedisPool;
        this.userContainer = database.getContainer(config.getCosmosDbConfig().userContainer);
        this.auth = new Auth2(config, jedisPool, database);
    }

    @Override
    public Result<UserItem, ServiceError> createUser(CreateUserParams params) {
        if (params.id().isBlank() || params.name().isBlank() || params.password().isBlank())
            return Result.err(ServiceError.BAD_REQUEST);

        var userDao = new UserDAO(
                params.id(),
                params.name(),
                Azure.hashUserPassword(params.password()),
                params.imageId().map(Azure::mediaIdToString).orElse(null),
                UserDAO.Status.ACTIVE);

        var createResult = Cosmos.createUser(this.userContainer, userDao);
        if (createResult.isError())
            return Result.err(createResult);

        AzureData.setUser(this.azureConfig, this.jedisPool, userDao);
        var userItem = AzureData.userDaoToItem(userDao);

        return Result.ok(userItem);
    }

    @Override
    public Result<UserItem, ServiceError> getUser(String userId) {
        if (userId.isBlank())
            return Result.err(ServiceError.BAD_REQUEST);

        var userResult = AzureData.getUser(this.azureConfig, this.jedisPool, this.userContainer, userId);
        if (userResult.isError())
            return Result.err(userResult);

        var userDao = userResult.value();
        var userItem = AzureData.userDaoToItem(userDao);

        return Result.ok(userItem);
    }

    @Override
    public Result<UserItem, ServiceError> deleteUser(SessionToken token, String userId) {
        var result = AzureData.getUser(this.azureConfig, this.jedisPool, this.userContainer, userId);
        if (result.isError())
            return Result.err(result);

        var userDao = result.value();
        var userItem = AzureData.userDaoToItem(userDao);
        MessageBus.sendDeleteUser(userId);

        return Result.ok(userItem);
    }

    @Override
    public Result<UserItem, ServiceError> updateUser(SessionToken token, String userId, UpdateUserOps ops) {
        var authResult = this.auth.match(token, userId);
        if (authResult.isError())
            return Result.err(authResult);

        var userDao = new UserDAO();
        if (ops.shouldUpdateName())
            userDao.setName(ops.getName());
        if (ops.shouldUpdatePassword())
            userDao.setHashedPwd(Azure.hashUserPassword(ops.getPassword()));
        if (ops.shouldUpdateImage())
            userDao.setPhotoId(Azure.mediaIdToString(ops.getImageId()));

        var updateResult = Cosmos.updateUser(this.userContainer, userId, userDao);
        if (updateResult.isError())
            return Result.err(updateResult);

        userDao = updateResult.value();
        AzureData.setUser(azureConfig, jedisPool, userDao);
        var userItem = AzureData.userDaoToItem(userDao);

        return Result.ok(userItem);
    }

    @Override
    public Result<SessionToken, ServiceError> authenticateUser(String userId, String password) {
        if (userId.isBlank() || password.isBlank())
            return Result.err(ServiceError.BAD_REQUEST);
        return this.auth.authenticate(userId, password);
    }

}
