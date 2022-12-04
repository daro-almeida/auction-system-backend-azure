package scc;

import scc.exception.ServiceException;

public interface MediaService extends AutoCloseable {
    /**
     * Uploads the given file to the media service.
     * 
     * @param namespace Namespace of the file.
     * @param contents  Contents of the file.
     * @return The identifier of the uploaded file.
     */
    MediaId uploadMedia(MediaNamespace namespace, byte[] contents) throws ServiceException;

    /**
     * Downloads the contents of the file with the given identifier.
     * 
     * @param mediaId Identifier of the file.
     * @return The contents of the file.
     */
    byte[] downloadMedia(MediaId mediaId) throws ServiceException;

    /**
     * Deletes the file with the given identifier.
     * 
     * @param mediaId Identifier of the file.
     * @return Ok if the file existed and was deleted, an error otherwise.
     */
    void deleteMedia(MediaId mediaId) throws ServiceException;
}
