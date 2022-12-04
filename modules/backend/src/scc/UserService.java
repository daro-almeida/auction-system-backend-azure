package scc;

import java.util.Optional;

import scc.exception.ServiceException;
import scc.item.UserItem;

public interface UserService extends AutoCloseable {
    public static record CreateUserParams(
            String username,
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
    UserItem createUser(CreateUserParams params) throws ServiceException;

    /**
     * Gets the user with the given id.
     * 
     * @param username the id of the user to get
     * @return the user with the given id
     */
    UserItem getUser(String username) throws ServiceException;

    /**
     * Deletes the user with the given id.
     * 
     * @param token    the session token of the user
     * @param username the id of the user to delete
     * @return The deleted user
     */
    UserItem deleteUser(SessionToken token, String username) throws ServiceException;

    /**
     * Updates the user with the given id.
     * 
     * @param token    the session token of the user
     * @param username the id of the user to update
     * @param ops      the operations to perform
     * @return the updated user
     */
    UserItem updateUser(SessionToken token, String username, UpdateUserOps ops) throws ServiceException;

    /**
     * Authenticates the user with the given id and password.
     * Previous session tokens are invalidated.
     * 
     * @param username the id of the user to authenticate
     * @param password the password of the user to authenticate
     * @return the session token of the authenticated user
     */
    SessionToken authenticateUser(String username, String password) throws ServiceException;
}
