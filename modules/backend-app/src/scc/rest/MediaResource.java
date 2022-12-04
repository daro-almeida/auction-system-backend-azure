package scc.rest;

import java.util.logging.Logger;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import scc.MediaNamespace;
import scc.MediaService;
import scc.ServiceFactory;

/**
 * Resource for managing media files, such as images.
 */
@Path("/media")
public class MediaResource {
    private static final Logger logger = Logger.getLogger(MediaResource.class.toString());

    private static final String MEDIA_ID = "media";
    private final ServiceFactory<MediaService> factory;

    public MediaResource(ServiceFactory<MediaService> factory) {
        this.factory = factory;
    }

    /**
     * Post a new image.The id of the image is its hash.
     */
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public String upload(byte[] contents) throws Exception {
        logger.fine("POST /media/ " + contents.length + " bytes");
        try (var service = this.factory.createService()) {
            var mediaId = service.uploadMedia(MediaNamespace.Auction, contents);
            return ResourceUtils.mediaIdToString(mediaId);
        }
    }

    /**
     * Return the contents of an image. Throw an appropriate error message if
     * id does not exist.
     */
    @GET
    @Path("/{" + MEDIA_ID + "}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public byte[] download(@PathParam(MEDIA_ID) String id) throws Exception {
        logger.fine("GET /media/" + id);
        try (var service = this.factory.createService()) {
            var mediaId = ResourceUtils.stringToMediaId(id);
            var content = service.downloadMedia(mediaId);
            return content;
        }
    }
}
