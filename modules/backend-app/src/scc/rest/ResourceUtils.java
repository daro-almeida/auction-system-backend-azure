package scc.rest;

import java.util.Base64;
import java.util.Optional;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.NewCookie;
import scc.MediaId;
import scc.MediaNamespace;
import scc.ServiceError;
import scc.SessionToken;

public class ResourceUtils {
    public static final String SESSION_COOKIE = "scc-session";

    public static NewCookie createSessionCookie(SessionToken sessionToken) {
        return new NewCookie.Builder(SESSION_COOKIE)
                .value(sessionToken.getToken())
                .comment("Session cookie for SCC")
                .maxAge(30 * 60)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .build();
    }

    public static SessionToken sessionTokenOrFail(Cookie authCookie) {
        if (authCookie == null)
            throw new WebApplicationException("Missing authentication cookie", 401);
        return new SessionToken(authCookie.getValue());
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

    public static String mediaNamespaceToString(MediaNamespace namespace) {
        return switch (namespace) {
            case User -> "user";
            case Auction -> "auction";
            default -> throw new IllegalArgumentException("Unknown media namespace");
        };
    }

    public static String mediaIdToString(MediaId mediaId) {
        return mediaNamespaceToString(mediaId.getNamespace()) + ":" + mediaId.getId();
    }

    public static MediaId stringToMediaId(String mediaId) {
        String[] parts = mediaId.split(":");
        if (parts.length != 2)
            throw new BadRequestException("Invalid media id");
        MediaNamespace namespace = switch (parts[0]) {
            case "user" -> MediaNamespace.User;
            case "auction" -> MediaNamespace.Auction;
            default -> throw new BadRequestException("Invalid media id");
        };
        return new MediaId(namespace, parts[1]);
    }

    public static void throwError(ServiceError error, String message) {
        switch (error) {
            case AUCTION_NOT_FOUND -> throw new NotFoundException(message);
            case BAD_REQUEST -> throw new BadRequestException(message);
            case UNAUTHORIZED -> throw new WebApplicationException(message, 401);
            case INVALID_CREDENTIALS -> throw new WebApplicationException(message, 401);
            case QUESTION_ALREADY_REPLIED -> throw new WebApplicationException(message, 409);
            case QUESTION_NOT_FOUND -> throw new NotFoundException(message);
            case USER_ALREADY_EXISTS -> throw new WebApplicationException(message, 409);
            case USER_NOT_FOUND -> throw new NotFoundException(message);
            case BID_NOT_FOUND -> throw new NotFoundException(message);
            case INTERNAL_ERROR -> throw new InternalError(message);
            case MEDIA_NOT_FOUND -> throw new NotFoundException(message);
            case AUCTION_NOT_OPEN -> throw new WebApplicationException(message, 409);
            case BID_CONFLICT -> throw new WebApplicationException(message, 409);
        }
        throw new IllegalArgumentException("Unexpected value: " + error);
    }
}
