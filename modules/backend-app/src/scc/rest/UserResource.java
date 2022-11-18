package scc.rest;

import static scc.rest.ResourceUtils.SESSION_COOKIE;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import scc.AuctionService;
import scc.MediaId;
import scc.MediaNamespace;
import scc.MediaService;
import scc.UpdateUserOps;
import scc.UserService;
import scc.rest.dto.AuctionDTO;
import scc.rest.dto.UserDTO;

/**
 * Resource for managing users.
 */
@Path("/user")
public class UserResource {
    private static final Logger logger = Logger.getLogger(UserResource.class.toString());

    private static final String USER_ID = "userId";

    private final UserService service;
    private final AuctionService auctionService;
    private final MediaService mediaService;

    public UserResource(UserService service, AuctionService auctionService, MediaService mediaService) {
        this.service = service;
        this.auctionService = auctionService;
        this.mediaService = mediaService;
    }

    @GET
    @Path("/{" + USER_ID + "}")
    @Consumes(MediaType.APPLICATION_JSON)
    public UserDTO getUser(@PathParam(USER_ID) String id) {
        logger.fine("GET /user/" + id);

        if (id == null)
            throw new BadRequestException("User id cannot be null");

        var result = this.service.getUser(id);
        if (result.isError())
            ResourceUtils.throwError(result.error(), result.errorMessage());

        var userItem = result.value();
        var userDto = UserDTO.from(userItem);
        logger.fine("GET /user/" + id + " -> " + userDto);

        return userDto;
    }

    /**
     * JSON which represents the set of parameters required to create an user
     */
    public static record CreateUserRequest(
            @JsonProperty(required = true) String id,
            @JsonProperty(required = true) String name,
            @JsonProperty(required = true) String pwd,
            String photoBase64,
            String photoId) {
    }

    public static record CreateUserResponse(
            String id,
            String name,
            String pwd,
            String photoId) {
    }

    /**
     * Creates an user and inserts it into the database
     * 
     * @param request JSON file which has the necessary parameters to create an user
     * @return Created user's nickname
     */
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public CreateUserResponse createUser(CreateUserRequest request) {
        logger.fine("POST /user/ " + request);

        if (request.photoBase64 != null && request.photoId != null)
            throw new BadRequestException("Only one of photoBase64 and photoId should be provided");

        Optional<MediaId> photoId = Optional.empty();
        if (request.photoBase64 != null) {
            var content = ResourceUtils.decodeBase64(request.photoBase64);
            var result = this.mediaService.uploadMedia(MediaNamespace.User, content);
            if (result.isError())
                ResourceUtils.throwError(result.error(), result.errorMessage());
            photoId = Optional.of(result.value());
        } else if (request.photoId != null) {
            photoId = Optional.of(ResourceUtils.stringToMediaId(request.photoId));
        }

        var createUserParams = new UserService.CreateUserParams(
                request.id,
                request.name,
                request.pwd,
                photoId);

        var result = this.service.createUser(createUserParams);
        if (result.isError())
            ResourceUtils.throwError(result.error(), result.errorMessage());
        var userItem = result.value();
        logger.fine("POST /user/ " + request + " -> " + userItem);

        return new CreateUserResponse(
                userItem.getId(),
                userItem.getName(),
                request.pwd,
                userItem.getPhotoId().map(MediaId::toString).orElse(null));
    }

    public static record AuthenticateUserRequest(
            @JsonProperty(required = true) String user,
            @JsonProperty(required = true) String pwd) {
    }

    @POST
    @Path("/auth")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response authenticateUser(AuthenticateUserRequest request) {
        logger.fine("POST /user/auth " + request);
        var result = this.service.authenticateUser(request.user, request.pwd);
        if (result.isError())
            ResourceUtils.throwError(result.error(), result.errorMessage());

        var sessionToken = result.value();
        var cookie = ResourceUtils.createSessionCookie(sessionToken);
        logger.fine("POST /user/auth " + request + " -> " + sessionToken);

        return Response.ok().cookie(cookie).build();
    }

    /**
     * JSON which represents the set of parameters to update on the user
     */
    public static record UpdateUserRequest(String name, String password, String imageId) {
    }

    /**
     * Updates the values saved in the user with given nickname to the new values
     * from the request
     * 
     * @param id             nickname of the user to be updated
     * @param request        Request that has the new parameters
     * @param authentication Cookie related to the user being "logged" in the
     *                       application
     */
    @PATCH
    @Path("/{" + USER_ID + "}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateUser(@PathParam(USER_ID) String id,
            UpdateUserRequest request,
            @CookieParam(SESSION_COOKIE) Cookie authentication) {
        logger.fine("PATCH /user/" + id + " " + request);
        if (id == null)
            throw new BadRequestException("User id cannot be null");

        var sessionToken = ResourceUtils.sessionTokenOrFail(authentication);

        var ops = new UpdateUserOps();
        if (request.name != null)
            ops.updateName(request.name);
        if (request.password != null)
            ops.updatePassword(request.password);
        if (request.imageId != null) {
            var mediaId = ResourceUtils.stringToMediaId(request.imageId);
            ops.updateImage(mediaId);
        }

        logger.fine("PATCH /user/" + id + " " + request + " -> " + ops);
        var result = this.service.updateUser(sessionToken, id, ops);
        if (result.isError())
            ResourceUtils.throwError(result.error(), result.errorMessage());
    }

    /**
     * Deletes a user from the database
     * 
     * @param id             nickname of the user wished to be deleted
     * @param authentication Cookie related to the user being "logged" in the
     *                       application
     */
    @DELETE
    @Path("/{" + USER_ID + "}")
    public void deleteUser(@PathParam(USER_ID) String id,
            @CookieParam(SESSION_COOKIE) Cookie authentication) {
        logger.fine("DELETE /user/" + id);
        if (id == null)
            throw new BadRequestException("User id cannot be null");

        var sessionToken = ResourceUtils.sessionTokenOrFail(authentication);

        var result = this.service.deleteUser(sessionToken, id);
        if (result.isError())
            ResourceUtils.throwError(result.error(), result.errorMessage());

        logger.fine("DELETE /user/" + id + " -> " + result.value());
    }

    @GET
    @Path("/{" + USER_ID + "}/auctions")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<AuctionDTO> getUserAuctions(@PathParam(USER_ID) String id, @QueryParam("status") String status) {
        logger.fine("GET /user/" + id + "/auctions");

        if (id == null)
            throw new BadRequestException("User id cannot be null");

        var result = this.auctionService.listAuctionsOfUser(id, "OPEN".equals(status));
        if (result.isError())
            ResourceUtils.throwError(result.error(), result.errorMessage());

        var auctions = result.value();
        var auctionDaos = auctions.stream().map(AuctionDTO::from).collect(Collectors.toList());
        logger.fine("GET /user/" + id + "/auctions -> " + auctionDaos);

        return auctionDaos;
    }

    @GET
    @Path("/{" + USER_ID + "}/following")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<AuctionDTO> getUserFollowing(@PathParam(USER_ID) String id) {
        logger.fine("GET /user/" + id + "/following");

        if (id == null)
            throw new BadRequestException("User id cannot be null");

        var result = this.auctionService.listAuctionsFollowedByUser(id);
        if (result.isError())
            ResourceUtils.throwError(result.error(), result.errorMessage());

        var auctions = result.value();
        var auctionDaos = auctions.stream().map(AuctionDTO::from).collect(Collectors.toList());
        logger.fine("GET /user/" + id + "/following -> " + auctionDaos);

        return auctionDaos;
    }

}
