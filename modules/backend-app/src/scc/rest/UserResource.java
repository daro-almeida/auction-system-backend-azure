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
import scc.ServiceFactory;
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

    private static final String USER_NAME = "userId";

    private final ServiceFactory<UserService> userFactory;
    private final ServiceFactory<AuctionService> auctionFactory;

    public UserResource(ServiceFactory<UserService> userFactory, ServiceFactory<AuctionService> auctionService) {
        this.userFactory = userFactory;
        this.auctionFactory = auctionService;
    }

    @GET
    @Path("/{" + USER_NAME + "}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public UserDTO getUser(@PathParam(USER_NAME) String id) throws Exception {
        logger.fine("GET /user/" + id);

        if (id == null)
            throw new BadRequestException("User id cannot be null");

        try (var service = this.userFactory.createService()) {
            var user = service.getUser(id);
            var userDto = UserDTO.from(user);
            logger.fine("GET /user/" + id + " -> " + userDto);
            return userDto;
        }
    }

    /**
     * JSON which represents the set of parameters required to create an user
     */
    public static record CreateUserRequest(
            @JsonProperty(required = true) String id,
            @JsonProperty(required = true) String name,
            @JsonProperty(required = true) String pwd,
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
    public CreateUserResponse createUser(CreateUserRequest request) throws Exception {
        logger.fine("POST /user/ " + request);

        if (request.id == null || request.name == null || request.pwd == null)
            throw new BadRequestException("id, name and pwd are required");

        Optional<MediaId> photoId = Optional.empty();
        if (request.photoId != null)
            photoId = Optional.of(ResourceUtils.stringToMediaId(request.photoId));

        var createUserParams = new UserService.CreateUserParams(
                request.id,
                request.name,
                request.pwd,
                photoId);

        try (var service = this.userFactory.createService()) {
            var user = service.createUser(createUserParams);
            logger.fine("POST /user/ " + request + " -> " + user);

            return new CreateUserResponse(
                    user.getId(),
                    user.getName(),
                    request.pwd,
                    user.getPhotoId().map(ResourceUtils::mediaIdToString).orElse(null));
        }
    }

    public static record AuthenticateUserRequest(
            @JsonProperty(required = true) String user,
            @JsonProperty(required = true) String pwd) {
    }

    @POST
    @Path("/auth")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response authenticateUser(AuthenticateUserRequest request) throws Exception {
        logger.fine("POST /user/auth " + request);

        if (request.user == null || request.pwd == null)
            throw new BadRequestException("user and pwd are required");

        try (var service = this.userFactory.createService()) {
            var sessionToken = service.authenticateUser(request.user, request.pwd);
            var cookie = ResourceUtils.createSessionCookie(sessionToken);
            logger.fine("POST /user/auth " + request + " -> " + sessionToken);

            return Response.ok().cookie(cookie).build();
        }
    }

    /**
     * JSON which represents the set of parameters to update on the user
     */
    public static record UpdateUserRequest(String name, String pwd, String imageId) {
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
    @Path("/{" + USER_NAME + "}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateUser(@PathParam(USER_NAME) String id,
            UpdateUserRequest request,
            @CookieParam(SESSION_COOKIE) Cookie authentication) throws Exception {
        logger.fine("PATCH /user/" + id + " " + request);
        if (id == null)
            throw new BadRequestException("User id cannot be null");

        try (var service = this.userFactory.createService()) {
            var sessionToken = ResourceUtils.sessionTokenOrFail(authentication);

            var ops = new UpdateUserOps();
            if (request.name != null)
                ops.updateName(request.name);
            if (request.pwd != null)
                ops.updatePassword(request.pwd);
            if (request.imageId != null) {
                var mediaId = ResourceUtils.stringToMediaId(request.imageId);
                ops.updateImage(mediaId);
            }

            logger.fine("PATCH /user/" + id + " " + request + " -> " + ops);
            service.updateUser(sessionToken, id, ops);
        }
    }

    /**
     * Deletes a user from the database
     * 
     * @param id             nickname of the user wished to be deleted
     * @param authentication Cookie related to the user being "logged" in the
     *                       application
     */
    @DELETE
    @Path("/{" + USER_NAME + "}")
    public void deleteUser(@PathParam(USER_NAME) String id,
            @CookieParam(SESSION_COOKIE) Cookie authentication) throws Exception {
        logger.fine("DELETE /user/" + id);
        if (id == null)
            throw new BadRequestException("User id cannot be null");

        var sessionToken = ResourceUtils.sessionTokenOrFail(authentication);

        try (var service = this.userFactory.createService()) {
            var user = service.deleteUser(sessionToken, id);
            logger.fine("DELETE /user/" + id + " -> " + user);
        }
    }

    @GET
    @Path("/{" + USER_NAME + "}/auctions")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<AuctionDTO> getUserAuctions(@PathParam(USER_NAME) String id, @QueryParam("status") String status)
            throws Exception {
        logger.fine("GET /user/" + id + "/auctions");

        if (id == null)
            throw new BadRequestException("User id cannot be null");

        try (var service = this.auctionFactory.createService()) {
            var auctions = service.listUserAuctions(id, "OPEN".equals(status));
            var auctionDtos = auctions.stream().map(AuctionDTO::from).collect(Collectors.toList());
            logger.fine("GET /user/" + id + "/auctions -> " + auctionDtos);
            return auctionDtos;
        }
    }

    @GET
    @Path("/{" + USER_NAME + "}/following")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<AuctionDTO> getUserFollowing(@PathParam(USER_NAME) String id) throws Exception {
        logger.fine("GET /user/" + id + "/following");

        if (id == null)
            throw new BadRequestException("User id cannot be null");

        try (var service = this.auctionFactory.createService()) {
            var auctions = service.listAuctionsFollowedByUser(id);
            var auctionDaos = auctions.stream().map(AuctionDTO::from).collect(Collectors.toList());
            logger.fine("GET /user/" + id + "/following -> " + auctionDaos);
            return auctionDaos;
        }
    }

}
