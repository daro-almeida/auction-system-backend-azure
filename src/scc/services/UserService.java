package scc.services;

import java.util.Optional;

import scc.services.data.UserItem;
import scc.utils.Result;

public interface UserService {
    public static record CreateUserParams(
            String nickname,
            String name,
            String password,
            Optional<byte[]> image) {
    }

    Result<UserItem, ServiceError> createUser(CreateUserParams params);

    Result<Void, ServiceError> deleteUser(String userId, String sessionToken);

    public static class UpdateUserOps {
        private String name;
        private String password;
        private byte[] image;

        public UpdateUserOps() {
        }

        public boolean shouldUpdateName() {
            return this.name != null;
        }

        public UpdateUserOps updateName(String name) {
            this.name = name;
            return this;
        }

        public String getName() {
            return this.name;
        }

        public boolean shouldUpdatePassword() {
            return this.password != null;
        }

        public UpdateUserOps updatePassword(String password) {
            this.password = password;
            return this;
        }

        public String getPassword() {
            return this.password;
        }

        public boolean shouldUpdateImage() {
            return this.image != null;
        }

        public UpdateUserOps updateImage(byte[] image) {
            this.image = image;
            return this;
        }

        public byte[] getImage() {
            return this.image;
        }
    }

    Result<Void, ServiceError> updateUser(String userId, UpdateUserOps ops, String sessionToken);

    /**
     * Authenticates a user.
     * 
     * @param userId   The user's id.
     * @param password The user's password.
     * @return A session token if the authentication was successful, or an error
     *         otherwise.
     */
    Result<String, ServiceError> authenticateUser(String userId, String password);

    // TODO: maybe make abstract class or wrapper class that already checks the
    // parameters instead of calling this functions on every service
    public static Result<Void, ServiceError> validateCreateUserParams(CreateUserParams params) {
        if (params.nickname == null || params.nickname.isBlank()) {
            return Result.err(ServiceError.BAD_REQUEST, "nickname is required");
        }
        if (params.name == null || params.name.isBlank()) {
            return Result.err(ServiceError.BAD_REQUEST, "name is required");
        }
        if (params.password == null || params.password.isBlank()) {
            return Result.err(ServiceError.BAD_REQUEST, "password is required");
        }
        return Result.ok();
    }

    public static Result<Void, ServiceError> validateUpdateUserOps(UpdateUserOps ops) {
        if (!ops.shouldUpdateName() && !ops.shouldUpdatePassword() && !ops.shouldUpdateImage()) {
            return Result.err(ServiceError.BAD_REQUEST, "no update operations specified");
        }
        if (ops.shouldUpdateName()) {
            if (ops.getName() == null || ops.getName().isBlank()) {
                return Result.err(ServiceError.BAD_REQUEST, "name is required");
            }
        }
        if (ops.shouldUpdatePassword()) {
            if (ops.getPassword() == null || ops.getPassword().isBlank()) {
                return Result.err(ServiceError.BAD_REQUEST, "password is required");
            }
        }
        return Result.ok();
    }
}
