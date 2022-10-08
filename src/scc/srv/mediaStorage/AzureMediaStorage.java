package scc.srv.mediaStorage;

import java.util.List;
import java.util.stream.Collectors;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobItem;

import scc.utils.Hash;

public class AzureMediaStorage implements MediaStorage {
	private final BlobContainerClient blobClient;

	public AzureMediaStorage(String connectionString, String containerName) {
		this.blobClient = new BlobContainerClientBuilder()
				.connectionString(connectionString)
				.containerName(containerName).buildClient();
	}

	@Override
	public String upload(byte[] contents) {
		var key = Hash.of(contents);
		var blob = this.blobClient.getBlobClient(key);
		blob.upload(BinaryData.fromBytes(contents), false);
		return key;
	}

	@Override
	public byte[] download(String mediaID) {
		var blob = this.blobClient.getBlobClient(mediaID);
		return blob.downloadContent().toBytes();
	}

	@Override
	public List<String> list() {
		return this.blobClient.listBlobs().stream().map(BlobItem::getName).collect(Collectors.toList());
	}

}
