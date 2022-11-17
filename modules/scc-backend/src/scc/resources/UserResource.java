package scc.resources;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import scc.services.AuctionService;
import scc.services.UserService;
import scc.services.data.AuctionItem;

import static scc.resources.ResourceUtils.SESSION_COOKIE;

/**
 * Resource for managing users.
 */
@Path("/user")
public class UserResource {

    private static final String USER_ID = "userId";

    private final UserService service;
    private final AuctionService auctionService;

    public UserResource(UserService service, AuctionService auctionService) {
        this.service = service;
        this.auctionService = auctionService;
    }

    /**
     * JSON which represents the set of parameters required to create an user
     */
    public static record CreateUserRequest(
            @JsonProperty(required = true) String id,
            @JsonProperty(required = true) String name,
            @JsonProperty(required = true) String pwd,
            String imageBase64,
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
        System.out.println("Received create user request");
        System.out.println(request);

        if (request.photoId != null && request.imageBase64 != null)
            throw new BadRequestException("Cannot specify both imageId and imageBase64");

        var result = this.service.createUser(new UserService.CreateUserParams(
                request.id(),
                request.name(),
                request.pwd(),
                ResourceUtils.decodeBase64Nullable(request.imageBase64),
                Optional.ofNullable(request.photoId)));

        if (result.isError())
            ResourceUtils.throwError(result.error(), result.errorMessage());

        var userItem = result.value();
        var response = new CreateUserResponse(
                userItem.getId(),
                userItem.getName(),
                request.pwd,
                userItem.getPhotoId().orElse(null));
        return response;
    }

    @GET
    @Path("/{" + USER_ID + "}/auctions")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getUserAuctions(@PathParam(USER_ID) String userId) { // TODO change do DTO
        System.out.println("Received get user auctions request");
        System.out.println("User ID: " + userId);

        var result = this.auctionService.listAuctionsOfUser(userId);

        if (result.isError())
            ResourceUtils.throwError(result.error(), result.errorMessage());

        return result.value().stream().map(AuctionItem::getId).toList();
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
        System.out.println("Received delete user request for id " + id);
        var sessionToken = ResourceUtils.sessionTokenOrFail(authentication);
        var result = this.service.deleteUser(id, sessionToken);
        if (result.isError())
            ResourceUtils.throwError(result.error(), result.errorMessage());
    }

    /**
     * JSON which represents the set of parameters to update on the user
     */
    public static record UpdateUserRequest(String name, String password, String imageBase64) {
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
        System.out.println("Received update user request for id " + id);
        System.out.println(request);

        var sessionToken = ResourceUtils.sessionTokenOrFail(authentication);
        var photo = ResourceUtils.decodeBase64Nullable(request.imageBase64());

        var updateOps = new UserService.UpdateUserOps();
        if (request.name() != null)
            updateOps = updateOps.updateName(request.name());

        if (request.password() != null)
            updateOps = updateOps.updatePassword(request.password());

        if (photo.isPresent())
            updateOps = updateOps.updateImage(photo.get());

        var result = this.service.updateUser(id, updateOps, sessionToken);

        if (result.isError())
            ResourceUtils.throwError(result.error(), result.errorMessage());
    }

    public static record AuthenticateUserRequest(
            @JsonProperty(required = true) String user,
            @JsonProperty(required = true) String pwd) {
    }

    @POST
    @Path("/auth")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response authenticateUser(AuthenticateUserRequest request) {
        System.out.println("Received authenticate user request");
        System.out.println(request);

        var result = this.service.authenticateUser(request.user(), request.pwd());

        if (result.isError())
            ResourceUtils.throwError(result.error(), result.errorMessage());

        var sessionToken = result.value();
        var cookie = ResourceUtils.createSessionCookie(sessionToken);

        return Response.ok().cookie(cookie).build();
    }
}
