package scc.utils;

import java.util.List;

import scc.SessionToken;
import scc.UpdateUserOps;
import scc.UserService;
import scc.exception.ServiceException;
import scc.item.UserItem;

public class UserServiceWithResources implements UserService {

    private final UserService userService;
    private final List<AutoCloseable> resources;

    public UserServiceWithResources(UserService userService, List<AutoCloseable> resources) {
        this.userService = userService;
        this.resources = resources;
    }

    @Override
    public void close() throws Exception {
        for (var resource : this.resources)
            resource.close();
    }

    @Override
    public UserItem createUser(CreateUserParams params) throws ServiceException {
        return this.userService.createUser(params);
    }

    @Override
    public UserItem getUser(String username) throws ServiceException {
        return this.userService.getUser(username);
    }

    @Override
    public UserItem deleteUser(SessionToken token, String username) throws ServiceException {
        return this.userService.deleteUser(token, username);
    }

    @Override
    public UserItem updateUser(SessionToken token, String username, UpdateUserOps ops) throws ServiceException {
        return this.userService.updateUser(token, username, ops);
    }

    @Override
    public SessionToken authenticateUser(String username, String password) throws ServiceException {
        return this.userService.authenticateUser(username, password);
    }

}
