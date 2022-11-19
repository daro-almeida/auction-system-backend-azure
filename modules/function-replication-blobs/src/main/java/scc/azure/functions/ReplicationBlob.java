package scc.azure.functions;

import com.azure.core.util.BinaryData;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.*;
import scc.MediaNamespace;
import scc.azure.MediaStorage;
import scc.azure.config.AzureEnv;

public class ReplicationBlob {
    @FunctionName("ReplicationBlob")
    public void ReplicationBlob(
            @BlobTrigger(
                name = "ReplicationBlob",
                dataType = "binary",
                path = "images/{name}",
                connection = "BlobStoreConnection")
            byte[] content,
            @BindingName("name") String blobName,
            final ExecutionContext context) {
        context.getLogger().info("Processing blob replication on blob :" + blobName);

        var blobStoreConfig = AzureEnv.getAzureBlobStoreConfig();
        var mediaStorage = new MediaStorage(blobStoreConfig);

        var string = blobName.split(":");
        var namespace = MediaNamespace.User;
        if ("auction".equals(string[0])) {
            namespace = MediaNamespace.Auction;
        }
        var client = mediaStorage.containerFromNamespace(namespace);
        var blob = client.getBlobClient(blobName);
        if (!blob.exists())
            blob.upload(BinaryData.fromBytes(content), false);
    }
}
