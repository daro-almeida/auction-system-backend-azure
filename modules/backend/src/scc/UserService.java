package scc;

import java.util.Optional;

import scc.item.UserItem;

public interface UserService {
    public static record CreateUserParams(
            String id,
            String name,
            String password,
            Optional<MediaId> imageId) {
    }

    /**
     * Creates a new user.
     * The user id must be unique and not empty.
     * The name must be not empty.
     * The password must be not empty.
     * 
     * @param params the parameters of the user to create
     * @return the created user
     */
    Result<UserItem, ServiceError> createUser(CreateUserParams params);

    /**
     * Gets the user with the given id.
     * 
     * @param userId the id of the user to get
     * @return the user with the given id
     */
    Result<UserItem, ServiceError> getUser(String userId);

    /**
     * Deletes the user with the given id.
     * 
     * @param token  the session token of the user
     * @param userId the id of the user to delete
     * @return The deleted user
     */
    Result<UserItem, ServiceError> deleteUser(SessionToken token, String userId);

    /**
     * Updates the user with the given id.
     * 
     * @param token  the session token of the user
     * @param userId the id of the user to update
     * @param ops    the operations to perform
     * @return the updated user
     */
    Result<UserItem, ServiceError> updateUser(SessionToken token, String userId, UpdateUserOps ops);

    /**
     * Authenticates the user with the given id and password.
     * Previous session tokens are invalidated.
     * 
     * @param userId   the id of the user to authenticate
     * @param password the password of the user to authenticate
     * @return the session token of the authenticated user
     */
    Result<SessionToken, ServiceError> authenticateUser(String userId, String password);
}
