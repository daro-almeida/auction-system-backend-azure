package scc;

public interface MediaService {
    /**
     * Uploads the given file to the media service.
     * 
     * @param namespace Namespace of the file.
     * @param contents  Contents of the file.
     * @return The identifier of the uploaded file.
     */
    Result<MediaId, ServiceError> uploadMedia(MediaNamespace namespace, byte[] contents);

    /**
     * Downloads the contents of the file with the given identifier.
     * 
     * @param mediaId Identifier of the file.
     * @return The contents of the file.
     */
    Result<byte[], ServiceError> downloadMedia(MediaId mediaId);

    /**
     * Deletes the file with the given identifier.
     * 
     * @param mediaId Identifier of the file.
     * @return Ok if the file existed and was deleted, an error otherwise.
     */
    Result<Void, ServiceError> deleteMedia(MediaId mediaId);
}
