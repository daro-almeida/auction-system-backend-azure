package scc.services;

import java.util.Optional;

public interface MediaService {
    String uploadAuctionMedia(byte[] contents);

    String uploadUserProfilePicture(byte[] contents);

    Optional<byte[]> downloadMedia(String mediaId);

    void deleteMedia(String mediaId);
}
