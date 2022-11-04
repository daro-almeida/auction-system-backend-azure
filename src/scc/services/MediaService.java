package scc.services;

import java.util.Optional;

public interface MediaService {
    public String uploadAuctionMedia(byte[] contents);

    public String uploadUserProfilePicture(String userId, byte[] contents);

    public Optional<byte[]> downloadMedia(String id);

    public boolean deleteMedia(String id);
}
