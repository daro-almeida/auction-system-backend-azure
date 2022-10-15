package scc.azure;

import java.util.Optional;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;

import scc.azure.config.BlobStoreConfig;
import scc.utils.Hash;

public class MediaStorage {
    private final String AUCTION_PREFIX = "auction:";
    private final String USER_PREFIX = "user:";

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

    public String uploadAuctionMedia(byte[] contents) {
        var key = this.createAuctionMediaID(contents);
        var blob = this.auctionClient.getBlobClient(key);
        if (!blob.exists())
            blob.upload(BinaryData.fromBytes(contents), true);
        return key;
    }

    public String uploadUserMedia(byte[] contents) {
        var key = this.createUserMediaID(contents);
        var blob = this.userClient.getBlobClient(key);
        if (!blob.exists())
            blob.upload(BinaryData.fromBytes(contents), true);
        return key;
    }

    public Optional<byte[]> downloadMedia(String mediaID) {
        var client = this.getClientFromID(mediaID);
        if (client == null)
            return Optional.empty();

        var blob = client.getBlobClient(mediaID);
        if (blob.exists())
            return Optional.of(blob.downloadContent().toBytes());
        else
            return Optional.empty();
    }

    public boolean deleteMedia(String mediaID) {
        var client = this.getClientFromID(mediaID);
        if (client == null)
            return false;

        var blob = client.getBlobClient(mediaID);
        if (blob.exists()) {
            blob.delete();
            return true;
        } else {
            return false;
        }
    }

    private String createAuctionMediaID(byte[] data) {
        return AUCTION_PREFIX + Hash.of(data);
    }

    private String createUserMediaID(byte[] data) {
        return USER_PREFIX + Hash.of(data);
    }

    private BlobContainerClient getClientFromID(String mediaID) {
        if (mediaID.startsWith(AUCTION_PREFIX))
            return this.auctionClient;
        else if (mediaID.startsWith(USER_PREFIX))
            return this.userClient;
        else
            return null;
    }
}
