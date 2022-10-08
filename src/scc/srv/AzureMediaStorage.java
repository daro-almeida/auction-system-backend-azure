package scc.srv;

import java.util.List;
import java.util.stream.Collectors;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobItem;

import scc.utils.Hash;

public class AzureMediaStorage implements MediaStorage {
	private final BlobContainerClient blob_client;

	public AzureMediaStorage(String connection_string, String container_name) {
		this.blob_client = new BlobContainerClientBuilder()
				.connectionString(connection_string)
				.containerName(container_name).buildClient();
	}

	@Override
	public String upload(byte[] contents) {
		var key = Hash.of(contents);
		var blob = this.blob_client.getBlobClient(key);
		blob.upload(BinaryData.fromBytes(contents), true);
		return key;
	}

	@Override
	public byte[] download(String media_id) {
		var blob = this.blob_client.getBlobClient(media_id);
		return blob.downloadContent().toBytes();
	}

	@Override
	public List<String> list() {
		return this.blob_client.listBlobs().stream().map(BlobItem::getName).collect(Collectors.toList());
	}

}
