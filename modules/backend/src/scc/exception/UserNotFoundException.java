package scc.exception;

public class UserNotFoundException extends ServiceException {
    public UserNotFoundException() {
        super("User not found");
    }

    public UserNotFoundException(String username) {
        super("User not found: " + username);
    }
}
