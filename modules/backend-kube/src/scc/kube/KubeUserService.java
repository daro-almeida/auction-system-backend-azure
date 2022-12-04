package scc.kube;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import scc.SessionToken;
import scc.UpdateUserOps;
import scc.UserService;
import scc.exception.BadRequestException;
import scc.exception.ServiceException;
import scc.item.UserItem;
import scc.kube.dao.UserDao;

public class KubeUserService implements UserService {

    private final Auth auth;
    private final KubeRepo repo;

    public KubeUserService(Auth auth, KubeRepo repo) {
        this.auth = auth;
        this.repo = repo;
    }

    @Override
    public UserItem createUser(CreateUserParams params) throws ServiceException {
        this.validateCreateUserParams(params);

        var userDao = new UserDao();
        userDao.username = params.username();
        userDao.name = params.name();
        userDao.hashedPassword = Kube.hashUserPassword(params.password());
        userDao.status = UserDao.Status.ACTIVE;
        userDao.createTime = LocalDateTime.now(ZoneOffset.UTC);
        userDao = this.repo.createUser(params);

        return this.userDaoToItem(userDao);
    }

    @Override
    public UserItem getUser(String username) throws ServiceException {
        if (username.isBlank())
            throw new BadRequestException("username is blank");

        var userId = this.repo.getUserIdFromUsername(username);
        var userDao = this.repo.getUser(userId);

        return this.userDaoToItem(userDao);
    }

    @Override
    public UserItem deleteUser(SessionToken token, String username) throws ServiceException {
        if (username.isBlank())
            throw new BadRequestException("username is blank");

        this.auth.validate(token, username);

        var userId = this.repo.getUserIdFromUsername(username);
        var userDao = this.repo.deactivateUser(userId);
        // TODO: rabbit send deactivate user

        return this.userDaoToItem(userDao);
    }

    @Override
    public UserItem updateUser(SessionToken token, String username, UpdateUserOps ops) throws ServiceException {
        if (username.isBlank())
            throw new BadRequestException("username is blank");

        this.auth.validate(token, username);

        var userId = this.repo.getUserIdFromUsername(username);

        var userDao = new UserDao();
        if (ops.shouldUpdateName())
            userDao.name = ops.getName();
        if (ops.shouldUpdatePassword())
            userDao.hashedPassword = Kube.hashUserPassword(ops.getPassword());
        if (ops.shouldUpdateImage())
            userDao.profileImageId = Kube.mediaIdToString(ops.getImageId());

        userDao = this.repo.updateUser(userId, userDao);
        var userItem = this.userDaoToItem(userDao);

        return userItem;
    }

    @Override
    public SessionToken authenticateUser(String userName, String password) throws ServiceException {
        return this.auth.authenticate(userName, password);
    }

    @Override
    public void close() throws Exception {
    }

    private void validateCreateUserParams(CreateUserParams params) throws BadRequestException {
        if (params.username().isBlank() || params.name().isBlank() || params.password().isBlank())
            throw new BadRequestException();
    }

    @WithSpan
    private UserItem userDaoToItem(UserDao userDao) {
        return new UserItem(
                userDao.username,
                userDao.name,
                Optional.ofNullable(userDao.profileImageId).map(Kube::stringToMediaId));
    }

}
