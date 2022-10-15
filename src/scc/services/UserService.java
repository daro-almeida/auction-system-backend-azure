package scc.services;

import java.util.Optional;

import scc.utils.Result;

public interface UserService {
    public static enum Error {
        BAD_REQUEST,
        USER_NOT_FOUND,
        USER_ALREADY_EXISTS,
    }

    public static record CreateUserParams(
            String nickname,
            String name,
            String password,
            Optional<byte[]> image) {
    }

    Result<String, Error> createUser(CreateUserParams params);

    Result<Void, Error> deleteUser(String userId);

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

    Result<Void, Error> updateUser(String userId, UpdateUserOps ops);

    public static Result<Void, Error> validateCreateUserParams(CreateUserParams params) {
        if (params.nickname == null || params.nickname.isBlank()) {
            return Result.err(Error.BAD_REQUEST, "nickname is required");
        }
        if (params.name == null || params.name.isBlank()) {
            return Result.err(Error.BAD_REQUEST, "name is required");
        }
        if (params.password == null || params.password.isBlank()) {
            return Result.err(Error.BAD_REQUEST, "password is required");
        }
        return Result.ok();
    }

    public static Result<Void, Error> validateUpdateUserOps(UpdateUserOps ops) {
        if (!ops.shouldUpdateName() && !ops.shouldUpdatePassword() && !ops.shouldUpdateImage()) {
            return Result.err(Error.BAD_REQUEST, "no update operations specified");
        }
        if (ops.shouldUpdateName()) {
            if (ops.getName() == null || ops.getName().isBlank()) {
                return Result.err(Error.BAD_REQUEST, "name is required");
            }
        }
        if (ops.shouldUpdatePassword()) {
            if (ops.getPassword() == null || ops.getPassword().isBlank()) {
                return Result.err(Error.BAD_REQUEST, "password is required");
            }
        }
        return Result.ok();
    }
}
