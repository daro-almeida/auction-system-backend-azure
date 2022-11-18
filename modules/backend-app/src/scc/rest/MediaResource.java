package scc.rest;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import scc.MediaNamespace;
import scc.MediaService;

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
        var result = this.service.uploadMedia(MediaNamespace.Auction, contents);
        if (result.isError())
            ResourceUtils.throwError(result.error(), result.errorMessage());

        var mediaId = result.value();
        return ResourceUtils.mediaIdToString(mediaId);
    }

    /**
     * Return the contents of an image. Throw an appropriate error message if
     * id does not exist.
     */
    @GET
    @Path("/{" + MEDIA_ID + "}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public byte[] download(@PathParam(MEDIA_ID) String id) {
        var mediaId = ResourceUtils.stringToMediaId(id);
        var result = this.service.downloadMedia(mediaId);
        if (result.isError())
            ResourceUtils.throwError(result.error(), result.errorMessage());

        return result.value();
    }
}
