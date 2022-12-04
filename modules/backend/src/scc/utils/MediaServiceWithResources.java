package scc.utils;

import java.util.List;

import scc.MediaId;
import scc.MediaNamespace;
import scc.MediaService;
import scc.exception.ServiceException;

public class MediaServiceWithResources implements MediaService {

    private final MediaService mediaService;
    private final List<AutoCloseable> resources;

    public MediaServiceWithResources(MediaService mediaService, List<AutoCloseable> resources) {
        this.mediaService = mediaService;
        this.resources = resources;
    }

    @Override
    public void close() throws Exception {
        for (var resource : this.resources)
            resource.close();
    }

    @Override
    public MediaId uploadMedia(MediaNamespace namespace, byte[] contents) throws ServiceException {
        return this.mediaService.uploadMedia(namespace, contents);
    }

    @Override
    public byte[] downloadMedia(MediaId mediaId) throws ServiceException {
        return this.mediaService.downloadMedia(mediaId);
    }

    @Override
    public void deleteMedia(MediaId mediaId) throws ServiceException {
        this.mediaService.deleteMedia(mediaId);
    }

}
