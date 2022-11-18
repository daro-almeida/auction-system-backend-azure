package scc;

public interface MediaService {
    Result<MediaId, ServiceError> uploadMedia(MediaNamespace namespace, byte[] contents);

    Result<byte[], ServiceError> downloadMedia(MediaId mediaId);

    Result<Void, ServiceError> deleteMedia(MediaId mediaId);
}
