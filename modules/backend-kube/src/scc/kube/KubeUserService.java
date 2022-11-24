package scc.kube;

import scc.Result;
import scc.ServiceError;
import scc.SessionToken;
import scc.UpdateUserOps;
import scc.UserService;
import scc.item.UserItem;

public class KubeUserService implements UserService {

    @Override
    public Result<UserItem, ServiceError> createUser(CreateUserParams params) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<UserItem, ServiceError> getUser(String userId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<UserItem, ServiceError> deleteUser(SessionToken token, String userId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<UserItem, ServiceError> updateUser(SessionToken token, String userId, UpdateUserOps ops) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<SessionToken, ServiceError> authenticateUser(String userId, String password) {
        // TODO Auto-generated method stub
        return null;
    }

}
