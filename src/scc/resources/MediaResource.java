package scc.resources;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import scc.services.MediaService;

/**
 * Resource for managing media files, such as images.
 */
@Path("/media")
public class MediaResource {
    private static final String MEDIA_ID = "media";
    private final MediaService service;

    public MediaResource(MediaService service) {
        this.service = service;
    }

    /**
     * Post a new image.The id of the image is its hash.
     */
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public String upload(byte[] contents) {
        return this.service.uploadMedia(contents);
    }

    /**
     * Return the contents of an image. Throw an appropriate error message if
     * id does not exist.
     */
    @GET
    @Path("/{" + MEDIA_ID + "}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public byte[] download(@PathParam(MEDIA_ID) String id) {
        var media = this.service.downloadMedia(id);
        if (media.isEmpty())
            throw new NotFoundException();
        return media.get();
    }
}
