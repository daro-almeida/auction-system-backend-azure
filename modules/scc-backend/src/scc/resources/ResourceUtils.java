package scc.resources;

import java.util.Base64;
import java.util.Optional;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.NewCookie;
import scc.services.ServiceError;

class ResourceUtils {
    public static final String SESSION_COOKIE = "scc:session";

    public static NewCookie createSessionCookie(String sessionToken) {
        return new NewCookie.Builder(SESSION_COOKIE)
                .value(sessionToken)
                .comment("Session cookie for SCC")
                .maxAge(30 * 60)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .build();
    }

    public static String sessionTokenOrFail(Cookie authCookie) {
        if (authCookie == null)
            throw new WebApplicationException("Missing authentication cookie", 401);
        return authCookie.getValue();
    }

    public static Optional<byte[]> decodeBase64Nullable(String base64) {
        if (base64 == null)
            return Optional.empty();
        return Optional.of(decodeBase64(base64));
    }

    public static byte[] decodeBase64(String base64) {
        try {
            return Base64.getDecoder().decode(base64);
        } catch (Exception e) {
            throw new BadRequestException("Invalid base64 string");
        }
    }

    public static void throwError(ServiceError error, String message) {
        switch (error) {
            case AUCTION_NOT_FOUND -> throw new NotFoundException(message);
            case BAD_REQUEST -> throw new BadRequestException(message);
            case INVALID_CREDENTIALS -> throw new WebApplicationException(message, 401);
            case QUESTION_ALREADY_REPLIED -> throw new WebApplicationException(message, 409);
            case QUESTION_NOT_FOUND -> throw new NotFoundException(message);
            case USER_ALREADY_EXISTS -> throw new WebApplicationException(message, 409);
            case USER_NOT_FOUND -> throw new NotFoundException(message);
            case BID_NOT_FOUND -> throw new NotFoundException(message);
            case INTERNAL_ERROR -> throw new InternalError(message);
        }
    }
}
