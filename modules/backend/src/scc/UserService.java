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

    Result<UserItem, ServiceError> createUser(CreateUserParams params);

    Result<UserItem, ServiceError> getUser(String userId);

    Result<UserItem, ServiceError> deleteUser(SessionToken token, String userId);

    Result<UserItem, ServiceError> updateUser(SessionToken token, String userId, UpdateUserOps ops);

    Result<SessionToken, ServiceError> authenticateUser(String userId, String password);
}
