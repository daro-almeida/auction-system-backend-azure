package scc.srv.resources;

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import scc.srv.mediaStorage.MediaStorage;

import static scc.srv.BuildConstants.*;

/**
 * Resource for managing media files, such as images.
 */
@Path("/media")
public class MediaResource {
	private final MediaStorage storage;

	public MediaResource(MediaStorage storage) {
		this.storage = storage;
	}

	/**
	 * Post a new image.The id of the image is its hash.
	 */
	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	@Produces(MediaType.APPLICATION_JSON)
	public String upload(byte[] contents) {
		return this.storage.upload(contents);
	}

	/**
	 * Return the contents of an image. Throw an appropriate error message if
	 * id does not exist.
	 */
	@GET
	@Path("/{" + MEDIA_ID + "}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public byte[] download(@PathParam(MEDIA_ID) String id) {
		return this.storage.download(id);
	}

	/**
	 * Lists the ids of images stored.
	 */
	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	public List<String> list() {
		return this.storage.list();
	}
}
