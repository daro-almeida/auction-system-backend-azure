package scc.memory;

import java.util.HashMap;
import java.util.Optional;

import org.apache.commons.lang3.NotImplementedException;

import scc.services.MediaService;
import scc.services.ServiceError;
import scc.services.UserService;
import scc.utils.Result;

public class MemoryUserService implements UserService {
    private static class User {
        public final String nickname;
        public String name;
        public String password;
        public Optional<String> imageId;

        public User(String nickname, String name, String password, Optional<String> imageId) {
            this.nickname = nickname;
            this.name = name;
            this.password = password;
            this.imageId = imageId;
        }
    }

    private final MediaService media;
    private final HashMap<String, User> users;

    public MemoryUserService(MediaService media) {
        this.media = media;
        this.users = new HashMap<>();
    }

    @Override
    public synchronized Result<String, ServiceError> createUser(CreateUserParams params) {
        var validateResult = UserService.validateCreateUserParams(params);
        if (validateResult.isError())
            return Result.err(validateResult.error());

        if (this.users.containsKey(params.nickname())) {
            return Result.err(ServiceError.USER_ALREADY_EXISTS);
        }

        var imageId = params.image().map(img -> this.media.uploadUserProfilePicture(params.nickname(), img));
        var user = new User(params.nickname(), params.name(), params.password(), imageId);
        this.users.put(params.nickname(), user);
        return Result.ok(params.nickname());
    }

    @Override
    public synchronized Result<Void, ServiceError> deleteUser(String userId) {
        var user = this.users.remove(userId);
        if (user == null) {
            return Result.err(ServiceError.USER_NOT_FOUND);
        }

        user.imageId.ifPresent(this.media::deleteMedia);
        return Result.ok();
    }

    @Override
    public synchronized Result<Void, ServiceError> updateUser(String userId, UpdateUserOps ops) {
        var validateResult = UserService.validateUpdateUserOps(ops);
        if (validateResult.isError())
            return Result.err(validateResult.error());

        var user = this.users.get(userId);
        if (user == null) {
            return Result.err(ServiceError.USER_NOT_FOUND);
        }

        if (ops.shouldUpdateName()) {
            user.name = ops.getName();
        }

        if (ops.shouldUpdatePassword()) {
            user.password = ops.getPassword();
        }

        if (ops.shouldUpdateImage()) {
            user.imageId.ifPresent(this.media::deleteMedia);
            user.imageId = Optional.of(this.media.uploadUserProfilePicture(userId, ops.getImage()));
        }

        return Result.ok();
    }

    public synchronized boolean userExists(String userId) {
        return this.users.containsKey(userId);
    }

    @Override
    public Result<String, ServiceError> authenticateUser(String userId, String password) {
        throw new NotImplementedException();
    }

}
