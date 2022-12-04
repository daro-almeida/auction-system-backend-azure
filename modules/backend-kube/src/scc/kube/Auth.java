package scc.kube;

import scc.SessionToken;
import scc.exception.BadCredentialsException;
import scc.exception.UnauthorizedException;
import scc.exception.UserNotFoundException;

public interface Auth extends AutoCloseable {
    SessionToken authenticate(String username, String password) throws UserNotFoundException, BadCredentialsException;

    String validate(SessionToken token) throws BadCredentialsException;

    String validate(SessionToken token, String username) throws BadCredentialsException, UnauthorizedException;
}
