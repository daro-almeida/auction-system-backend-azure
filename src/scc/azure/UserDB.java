package scc.azure;

import java.util.Optional;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosPatchOperations;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;

import scc.azure.config.CosmosDbConfig;
import scc.azure.dao.UserDAO;
import scc.services.UserService;
import scc.utils.Result;

class UserDB {
    private final CosmosContainer container;

    public UserDB(CosmosDatabase db, CosmosDbConfig config) {
        this.container = db.getContainer(config.userContainer);
    }

    public Optional<UserDAO> getUser(String userId) {
        var options = this.createQueryOptions(userId);
        return this.container
                .queryItems(
                        "SELECT * FROM users WHERE users.id=\"" + userId + "\"",
                        options,
                        UserDAO.class)
                .stream()
                .findFirst();
    }

    public boolean userExists(String userId) {
        return this.getUser(userId).isPresent();
    }

    public Result<UserDAO, UserService.Error> createUser(UserDAO user) {
        try {
            var response = this.container.createItem(user);
            return Result.ok(response.getItem());
        } catch (CosmosException e) {
            return Result.error(UserService.Error.USER_ALREADY_EXISTS);
        }
    }

    public Result<Void, UserService.Error> deleteUser(String userId) {
        var options = this.createRequestOptions(userId);
        var partitionKey = this.createPartitionKey(userId);
        try {
            var response = this.container.deleteItem(userId, partitionKey, options);
            return Result.ok();
        } catch (CosmosException e) {
            if (e.getStatusCode() == 404)
                return Result.error(UserService.Error.USER_NOT_FOUND);
            throw e;
        }
    }

    public Result<Void, UserService.Error> updateUser(String userId, CosmosPatchOperations ops) {
        var partitionKey = this.createPartitionKey(userId);
        try {
            this.container.patchItem(userId, partitionKey, ops, UserDAO.class);
            return Result.ok();
        } catch (CosmosException e) {
            if (e.getStatusCode() == 404)
                return Result.error(UserService.Error.USER_NOT_FOUND);
            throw e;
        }
    }

    private PartitionKey createPartitionKey(String userId) {
        return new PartitionKey(userId);
    }

    private CosmosItemRequestOptions createRequestOptions(String userId) {
        var options = new CosmosItemRequestOptions();
        return options;
    }

    private CosmosQueryRequestOptions createQueryOptions(String userId) {
        return new CosmosQueryRequestOptions().setPartitionKey(this.createPartitionKey(userId));
    }
}