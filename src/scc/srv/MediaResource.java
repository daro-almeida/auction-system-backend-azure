package scc.srv;

import scc.utils.Hash;

import java.util.stream.Collectors;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Resource for managing media files, such as images.
 */
@Path("/media")
public class MediaResource {
	private final BlobContainerClient blob_client;

	public MediaResource() {
		this.blob_client = new BlobContainerClientBuilder()
				.connectionString(BuildConstants.AZURE_STORAGE_ACC_CONNECTION_STRING)
				.containerName(BuildConstants.AZURE_STORAGE_CONTAINER_IMAGES).buildClient();
	}

	/**
	 * Post a new image.The id of the image is its hash.
	 */
	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	@Produces(MediaType.TEXT_PLAIN)
	public String upload(byte[] contents) {
		var key = Hash.of(contents);
		var blob = this.blob_client.getBlobClient(key);
		blob.upload(BinaryData.fromBytes(contents), true);
		return key;
	}

	/**
	 * Return the contents of an image. Throw an appropriate error message if
	 * id does not exist.
	 */
	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public byte[] download(@PathParam("id") String id) {
		var blob = this.blob_client.getBlobClient(id);
		return blob.downloadContent().toBytes();
	}

	/**
	 * Lists the ids of images stored.
	 */
	@GET
	@Path("/")
	@Produces(MediaType.TEXT_PLAIN)
	public String list() {
		return this.blob_client.listBlobs().stream().map(b -> b.getName()).collect(Collectors.toList()).toString();
	}
}
