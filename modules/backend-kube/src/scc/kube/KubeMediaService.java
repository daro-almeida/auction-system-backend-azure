package scc.kube;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Logger;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import scc.MediaId;
import scc.MediaNamespace;
import scc.MediaService;
import scc.exception.InternalErrorException;
import scc.exception.MediaNotFoundException;
import scc.exception.ServiceException;
import scc.kube.config.KubeMediaConfig;
import scc.utils.Hash;

public class KubeMediaService implements MediaService {
    private static final Logger logger = Logger.getLogger(KubeMediaService.class.getName());

    private final KubeMediaConfig config;

    public KubeMediaService(KubeMediaConfig config) throws IOException {
        this.config = config;

        java.nio.file.Files.createDirectories(Path.of(this.config.dataDirectory));
    }

    @Override
    @WithSpan
    public MediaId uploadMedia(MediaNamespace namespace, byte[] contents) throws ServiceException {
        var hash = Hash.of(contents);
        var mediaId = new MediaId(hash);
        var path = this.mediaIdToPath(mediaId);

        try {
            java.nio.file.Files.write(path, contents);
            logger.fine("Wrote media " + mediaId.toString() + "to" + path);
            return mediaId;
        } catch (IOException e) {
            logger.warning("Failed to write media file: " + e);
            throw new InternalErrorException("Failed to write media file", e);
        }
    }

    @Override
    @WithSpan
    public byte[] downloadMedia(MediaId mediaId) throws ServiceException {
        var filePath = this.mediaIdToPath(mediaId);
        if (!java.nio.file.Files.exists(filePath))
            throw new MediaNotFoundException(mediaId);

        try {
            var data = java.nio.file.Files.readAllBytes(filePath);
            return data;
        } catch (IOException e) {
            logger.warning("Failed to read media file: " + e.getMessage());
            throw new InternalErrorException("Failed to obtain media", e);
        }
    }

    @Override
    @WithSpan
    public void deleteMedia(MediaId mediaId) throws ServiceException {
        var filePath = this.mediaIdToPath(mediaId);
        if (!java.nio.file.Files.exists(filePath))
            throw new MediaNotFoundException(mediaId);

        try {
            java.nio.file.Files.delete(filePath);
        } catch (IOException e) {
            logger.warning("Failed to delete media file: " + e.getMessage());
            throw new InternalErrorException("Failed to delete media", e);
        }
    }

    @Override
    public void close() throws Exception {
        // TODO Auto-generated method stub

    }

    private Path mediaIdToPath(MediaId mediaId) {
        return Path.of(config.dataDirectory, mediaId.getId());
    }

}
