package scc.resources;

import java.util.Base64;
import java.util.Optional;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import scc.services.AuctionService;
import scc.services.UserService;

class ResourceUtils {
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

    /**
     * Throws an exception that corresponds to the error given
     * 
     * @param error Error code of the response
     */
    public static void throwError(UserService.Error error, String message) {
        switch (error) {
            case USER_NOT_FOUND:
                throw new NotFoundException();
            case USER_ALREADY_EXISTS:
                throw new WebApplicationException(409);
            case BAD_REQUEST:
                throw new BadRequestException(message);
        }
    }

    public static void throwError(AuctionService.Error error) {
        throwError(error, "");
    }

    public static void throwError(AuctionService.Error error, String message) {
        switch (error) {
            case AUCTION_NOT_FOUND, USER_NOT_FOUND, QUESTION_NOT_FOUND ->
                throw new NotFoundException();
            case QUESTION_ALREADY_REPLIED -> throw new WebApplicationException(409);
            case BAD_REQUEST -> throw new BadRequestException(message);
        }
    }
}
