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
import scc.services.ServiceError;
import scc.services.UserService;
import scc.utils.Result;

class UserDB {
    private final CosmosContainer container;

    public UserDB(CosmosDatabase db, CosmosDbConfig config) {
        this.container = db.getContainer(config.userContainer);
    }

    /**
     * Returns the user with given identifier from the database
     * 
     * @param userId Identifier of the user requested
     * @return Object which represents the user in the database
     */
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

    /**
     * Checks if a user with given identifier exists in the database
     * 
     * @param userId Identifier of the user requested
     * @return True if exists in the database, false otherwise
     */
    public boolean userExists(String userId) {
        return this.getUser(userId).isPresent();
    }

    /**
     * Saves the user in object form into the database
     * 
     * @param user Object that represents the user
     * @return 200 with created user's nickname or error if it already exists in the
     *         database
     */
    public Result<UserDAO, ServiceError> createUser(UserDAO user) {
        try {
            var response = this.container.createItem(user);
            return Result.ok(response.getItem());
        } catch (CosmosException e) {
            return Result.err(ServiceError.USER_ALREADY_EXISTS);
        }
    }

    /**
     * Deletes the user with given nickname from the database
     * 
     * @param userId nickname of the user to be deleted
     * @return 204 if successful, respective error otherwise
     */
    public Result<UserDAO, ServiceError> deleteUser(String userId) {
        var userOpt = this.getUser(userId);
        if (userOpt.isEmpty())
            return Result.err(ServiceError.USER_NOT_FOUND);
        var user = userOpt.get();

        var options = this.createRequestOptions(userId);
        var partitionKey = this.createPartitionKey(userId);
        try {
            this.container.deleteItem(userId, partitionKey, options);
            return Result.ok(user);
        } catch (CosmosException e) {
            if (e.getStatusCode() == 404)
                return Result.err(ServiceError.USER_NOT_FOUND);
            throw e;
        }
    }

    /**
     * Updates the values in the user with given nickname with new given values
     * 
     * @param userId nickname of the user to be updated
     * @param ops    operations to be executed on the user's database entry
     * @return 200 with updated user, respective error otherwise
     */
    public Result<UserDAO, ServiceError> updateUser(String userId, CosmosPatchOperations ops) {
        var partitionKey = this.createPartitionKey(userId);
        try {
            this.container.patchItem(userId, partitionKey, ops, UserDAO.class);
            var updatedUser = getUser(userId);
            return Result.ok(updatedUser.get());
        } catch (CosmosException e) {
            if (e.getStatusCode() == 404)
                return Result.err(ServiceError.USER_NOT_FOUND);
            throw e;
        }
    }

    public boolean userWithPhoto(String photoId) {
        Optional<Integer> numUsers = this.container
                .queryItems(
                        "SELECT VALUE COUNT(1) FROM users WHERE users.photoId=\"" + photoId + "\"",
                        new CosmosQueryRequestOptions(),
                        int.class)
                .stream()
                .findFirst();
        return numUsers.filter(num -> num > 0).isPresent();
    }

    /**
     * Creates a partition key with given nickaname to be used on database
     * operations
     * 
     * @param userId nickname of the user
     * @return PartitionKey object with user's nickname
     */
    private PartitionKey createPartitionKey(String userId) {
        return new PartitionKey(userId);
    }

    /**
     * Creates an ItemRequestOptions object with given nickname to be used on
     * database operations
     * 
     * @param userId nickname of the user
     * @return CosmosItemRequestOptions object with user's nickname
     */
    private CosmosItemRequestOptions createRequestOptions(String userId) {
        return new CosmosItemRequestOptions();
    }

    /**
     * Creates a QueryRequestOptions object with given nickname to be used on
     * database operations
     * 
     * @param userId nickname of the user
     * @return CosmosQueryRequestOptions object with user's nickname
     */
    private CosmosQueryRequestOptions createQueryOptions(String userId) {
        return new CosmosQueryRequestOptions().setPartitionKey(this.createPartitionKey(userId));
    }
}