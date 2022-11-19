package scc.azure;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;

import scc.MediaId;
import scc.MediaNamespace;
import scc.MediaService;
import scc.Result;
import scc.ServiceError;
import scc.azure.config.AzureConfig;
import scc.utils.Hash;

public class AzureMediaService implements MediaService {

    private final BlobContainerClient auctionClient;
    private final BlobContainerClient userClient;

    public AzureMediaService(AzureConfig config) {
        var blobConfig = config.getBlobStoreConfig();

        this.auctionClient = new BlobContainerClientBuilder()
                .connectionString(blobConfig.connectionString)
                .containerName(blobConfig.auctionContainer).buildClient();

        this.userClient = new BlobContainerClientBuilder()
                .connectionString(blobConfig.connectionString)
                .containerName(blobConfig.userContainer).buildClient();
    }

    @Override
    public Result<MediaId, ServiceError> uploadMedia(MediaNamespace namespace, byte[] contents) {
        var mediaId = new MediaId(namespace, Hash.of(contents));
        var key = Azure.mediaIdToString(mediaId);
        var client = this.containerFromNamespace(namespace);
        var blob = client.getBlobClient(key);
        if (!blob.exists())
            blob.upload(BinaryData.fromBytes(contents), false);
        return Result.ok(mediaId);
    }

    @Override
    public Result<byte[], ServiceError> downloadMedia(MediaId mediaId) {
        var client = this.containerFromNamespace(mediaId.getNamespace());
        var key = Azure.mediaIdToString(mediaId);
        var blob = client.getBlobClient(key);
        if (blob.exists())
            return Result.ok(blob.downloadContent().toBytes());
        else
            return Result.err(ServiceError.MEDIA_NOT_FOUND);
    }

    @Override
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

    private BlobContainerClient containerFromNamespace(MediaNamespace namespace) {
        return switch (namespace) {
            case Auction -> this.auctionClient;
            case User -> this.userClient;
            default -> throw new IllegalArgumentException("Invalid namespace");
        };
    }
}
