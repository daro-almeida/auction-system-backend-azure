package scc.kube;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Logger;

import scc.MediaId;
import scc.MediaNamespace;
import scc.MediaService;
import scc.Result;
import scc.ServiceError;
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
    public Result<MediaId, ServiceError> uploadMedia(MediaNamespace namespace, byte[] contents) {
        var hash = Hash.of(contents);
        var mediaId = new MediaId(hash);
        var path = this.mediaIdToPath(mediaId);

        try {
            java.nio.file.Files.write(path, contents);
            logger.fine("Wrote media " + mediaId.toString() + "to" + path);
            return Result.ok(mediaId);
        } catch (IOException e) {
            logger.warning("Failed to write media file: " + e);
            return Result.err(ServiceError.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<byte[], ServiceError> downloadMedia(MediaId mediaId) {
        var filePath = this.mediaIdToPath(mediaId);
        if (!java.nio.file.Files.exists(filePath))
            return Result.err(ServiceError.MEDIA_NOT_FOUND, "Media not found");

        try {
            var data = java.nio.file.Files.readAllBytes(filePath);
            return Result.ok(data);
        } catch (IOException e) {
            logger.warning("Failed to read media file: " + e.getMessage());
            return Result.err(ServiceError.INTERNAL_ERROR, "Failed to obtain media");
        }
    }

    @Override
    public Result<Void, ServiceError> deleteMedia(MediaId mediaId) {
        var filePath = this.mediaIdToPath(mediaId);
        if (!java.nio.file.Files.exists(filePath))
            return Result.err(ServiceError.MEDIA_NOT_FOUND, "Media not found");

        try {
            java.nio.file.Files.delete(filePath);
            return Result.ok();
        } catch (IOException e) {
            logger.warning("Failed to delete media file: " + e.getMessage());
            return Result.err(ServiceError.INTERNAL_ERROR, "Failed to delete media");
        }
    }

    private Path mediaIdToPath(MediaId mediaId) {
        return Path.of(config.dataDirectory, mediaId.getId());
    }
}
