package scc.azure;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;

import scc.MediaId;
import scc.MediaNamespace;
import scc.Result;
import scc.ServiceError;
import scc.azure.config.BlobStoreConfig;
import scc.utils.Hash;

public class MediaStorage {
    private final BlobContainerClient auctionClient;
    private final BlobContainerClient userClient;

    public MediaStorage(BlobStoreConfig config) {
        this.auctionClient = new BlobContainerClientBuilder()
                .connectionString(config.connectionString)
                .containerName(config.auctionContainer).buildClient();

        this.userClient = new BlobContainerClientBuilder()
                .connectionString(config.connectionString)
                .containerName(config.userContainer).buildClient();
    }

    /**
     * Upload an image into the blob storage container for auctions
     * 
     * @param contents byte array of an image
     * @return Uploaded image's generated identifier
     */
    public MediaId uploadMedia(MediaNamespace namespace, byte[] contents) {
        var mediaId = new MediaId(namespace, Hash.of(contents));
        var key = Azure.mediaIdToString(mediaId);
        var client = this.containerFromNamespace(namespace);
        var blob = client.getBlobClient(key);
        if (!blob.exists())
            blob.upload(BinaryData.fromBytes(contents), false);
        return mediaId;
    }

    /**
     * Downloads the contents of an image with given identifier
     * 
     * @param mediaID identifier of the media resource
     * @return byte content of the image
     */
    public Result<byte[], ServiceError> downloadMedia(MediaId mediaId) {
        var client = this.containerFromNamespace(mediaId.getNamespace());
        var key = Azure.mediaIdToString(mediaId);
        var blob = client.getBlobClient(key);
        if (blob.exists())
            return Result.ok(blob.downloadContent().toBytes());
        else
            return Result.err(ServiceError.MEDIA_NOT_FOUND);
    }

    /**
     * Deletes the contents of an image with given identifier
     * 
     * @param mediaID identifier of the media resource
     * @return true if it was deleted, false otherwise
     */
    public Result<Void, ServiceError> deleteMedia(MediaId mediaId) {
        var client = this.containerFromNamespace(mediaId.getNamespace());
        var key = Azure.mediaIdToString(mediaId);
        var blob = client.getBlobClient(key);
        if (blob.exists()) {
            blob.delete();
            return Result.ok();
        } else {
            return Result.err(ServiceError.MEDIA_NOT_FOUND);
        }
    }

    public BlobContainerClient containerFromNamespace(MediaNamespace namespace) {
        return switch (namespace) {
            case Auction -> this.auctionClient;
            case User -> this.userClient;
            default -> throw new IllegalArgumentException("Invalid namespace");
        };
    }
}
