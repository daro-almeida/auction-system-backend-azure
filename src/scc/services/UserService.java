package scc.services;

import java.util.Optional;

import scc.utils.Result;

public interface UserService {
    public static enum Error {
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
}
