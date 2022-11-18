package scc.azure.repo;

import scc.Result;
import scc.ServiceError;
import scc.azure.dao.UserDAO;

public interface UserRepo {
    public Result<UserDAO, ServiceError> getUser(String id);

    public Result<UserDAO, ServiceError> insertUser(UserDAO user);

    public Result<UserDAO, ServiceError> updateUser(UserDAO user);

    public Result<UserDAO, ServiceError> deleteUser(String id);
}
