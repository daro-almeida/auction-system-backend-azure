package scc.azure;

import scc.Result;
import scc.ServiceError;
import scc.azure.dao.UserDAO;

public interface DaoRepo {
    Result<UserDAO, ServiceError> getUser();

    void invalidateUser(String userId);
}
