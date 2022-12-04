package scc.exception;

import scc.MediaId;

public class MediaNotFoundException extends ServiceException {
    public MediaNotFoundException(MediaId mediaId) {
        super("Media not found: " + mediaId.toString());
    }
}
