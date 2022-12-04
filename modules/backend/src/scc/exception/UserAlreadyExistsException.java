package scc.exception;

public class UserAlreadyExistsException extends ServiceException {

    public UserAlreadyExistsException(String username) {
        super(String.format("User %s already exists", username));
    }

}
