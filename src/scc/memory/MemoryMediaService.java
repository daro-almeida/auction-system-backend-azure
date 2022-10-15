package scc.memory;

import java.util.HashMap;
import java.util.Optional;

import scc.services.MediaService;

public class MemoryMediaService implements MediaService {

    private final HashMap<Integer, byte[]> media = new HashMap<>();
    private int currentId = 0;

    @Override
    public synchronized String uploadMedia(byte[] contents) {
        int id = this.currentId++;
        this.media.put(id, contents);
        return Integer.toString(id);
    }

    @Override
    public synchronized String uploadUserProfilePicture(String userId, byte[] contents) {
        return this.uploadMedia(contents);
    }

    @Override
    public synchronized Optional<byte[]> downloadMedia(String id) {
        try {
            return Optional.ofNullable(this.media.get(Integer.parseInt(id)));
        } catch (NumberFormatException __) {
            return Optional.empty();
        }
    }

    @Override
    public synchronized boolean deleteMedia(String id) {
        var prev = this.media.remove(Integer.parseInt(id));
        return prev != null;
    }

}
