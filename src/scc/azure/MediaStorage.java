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

    /**
     * Upload an image into the blob storage container for auctions
     * 
     * @param contents byte array of an image
     * @return Uploaded image's generated identifier
     */
    public String uploadAuctionMedia(byte[] contents) {
        var key = this.createAuctionMediaID(contents);
        var blob = this.auctionClient.getBlobClient(key);
        if (!blob.exists())
            blob.upload(BinaryData.fromBytes(contents), false);
        return key;
    }

    /**
     * Upload an image into the blob storage container for users
     * 
     * @param contents byte array of an image
     * @return Uploaded image's generated identifier
     */
    public String uploadUserMedia(byte[] contents) {
        var key = this.createUserMediaID(contents);
        var blob = this.userClient.getBlobClient(key);
        if (!blob.exists())
            blob.upload(BinaryData.fromBytes(contents), false);
        return key;
    }

    /**
     * Downloads the contents of an image with given identifier
     * 
     * @param mediaID identifier of the media resource
     * @return byte content of the image
     */
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

    /**
     * Deletes the contents of an image with given identifier
     * 
     * @param mediaID identifier of the media resource
     * @return true if it was deleted, false otherwise
     */
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

    /**
     * Generates an identifier with prefix of "auction" to determine which blob
     * storage container it goes to
     * 
     * @param data contents of the media resource
     * @return media resource's generated identifier
     */
    public String createAuctionMediaID(byte[] data) {
        return AUCTION_PREFIX + Hash.of(data);
    }

    /**
     * Generates an identifier with prefix of "users" to determine which blob
     * storage container it goes to
     * 
     * @param data contents of the media resource
     * @return media resource's generated identifier
     */
    public String createUserMediaID(byte[] data) {
        return USER_PREFIX + Hash.of(data);
    }

    /**
     * Get the client to the respective blob storage container with given identifier
     * 
     * @param mediaID identifier of the media resource
     * @return client to the blob storage container or null if invalid
     */
    private BlobContainerClient getClientFromID(String mediaID) {
        if (mediaID.startsWith(AUCTION_PREFIX))
            return this.auctionClient;
        else if (mediaID.startsWith(USER_PREFIX))
            return this.userClient;
        else
            return null;
    }
}
