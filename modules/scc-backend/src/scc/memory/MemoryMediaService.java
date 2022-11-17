package scc.memory;

import java.util.HashMap;
import java.util.Optional;

import scc.services.MediaService;

public class MemoryMediaService implements MediaService {

    private final HashMap<Integer, byte[]> media = new HashMap<>();
    private int currentId = 0;

    @Override
    public synchronized String uploadAuctionMedia(byte[] contents) {
        int id = this.currentId++;
        this.media.put(id, contents);
        return Integer.toString(id);
    }

    @Override
    public synchronized String uploadUserProfilePicture(byte[] contents) {
        return this.uploadAuctionMedia(contents);
    }

    @Override
    public synchronized Optional<byte[]> downloadMedia(String mediaId) {
        try {
            return Optional.ofNullable(this.media.get(Integer.parseInt(mediaId)));
        } catch (NumberFormatException __) {
            return Optional.empty();
        }
    }

    @Override
    public synchronized void deleteMedia(String mediaId) {
        var prev = this.media.remove(Integer.parseInt(mediaId));
    }

}
