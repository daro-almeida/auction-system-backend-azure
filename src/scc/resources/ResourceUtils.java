package scc.resources;

import java.util.Base64;
import java.util.Optional;

import jakarta.ws.rs.BadRequestException;

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
}
