package scc.resources;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import scc.services.UserService;

/**
 * Resource for managing users.
 */
@Path("/user")
public class UserResource {

    private static final String USER_ID = "userId";

    private final UserService service;

    public UserResource(UserService service) {
        this.service = service;
    }

    /**
     * JSON which represents the set of parameters required to create an user
     */
    public static record CreateUserRequest(
            @JsonProperty(required = true) String nickname,
            @JsonProperty(required = true) String name,
            @JsonProperty(required = true) String password,
            String imageBase64) {
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
    public String createUser(CreateUserRequest request) {
        System.out.println("Received create user request");
        System.out.println(request);

        var result = this.service.createUser(new UserService.CreateUserParams(
                request.nickname(),
                request.name(),
                request.password(),
                ResourceUtils.decodeBase64Nullable(request.imageBase64)));

        if (result.isErr())
            this.throwUserError(result.error(), result.errorMessage());

        return result.value();
    }

    /**
     * Deletes a user from the database
     * 
     * @param id nickname of the user wished to be deleted
     */
    @DELETE
    @Path("/{" + USER_ID + "}")
    public void deleteUser(@PathParam(USER_ID) String id) {
        System.out.println("Received delete user request for id " + id);
        var result = this.service.deleteUser(id);
        if (result.isErr())
            this.throwUserError(result.error(), result.errorMessage());
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
     * @param id      nickname of the user to be updated
     * @param request Request that has the new parameters
     */
    @PATCH
    @Path("/{" + USER_ID + "}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateUser(@PathParam(USER_ID) String id, UpdateUserRequest request) {
        System.out.println("Received update user request for id " + id);
        System.out.println(request);

        var photo = ResourceUtils.decodeBase64Nullable(request.imageBase64());

        var updateOps = new UserService.UpdateUserOps();
        if (request.name() != null)
            updateOps = updateOps.updateName(request.name());

        if (request.password() != null)
            updateOps = updateOps.updatePassword(request.password());

        if (photo.isPresent())
            updateOps = updateOps.updateImage(photo.get());

        var result = this.service.updateUser(id, updateOps);

        if (result.isErr())
            this.throwUserError(result.error(), result.errorMessage());
    }

    /**
     * Throws an exception that corresponds to the error given
     * 
     * @param error Error code of the response
     */
    private void throwUserError(UserService.Error error, String message) {
        switch (error) {
            case USER_NOT_FOUND:
                throw new NotFoundException();
            case USER_ALREADY_EXISTS:
                throw new WebApplicationException(409);
            case BAD_REQUEST:
                throw new BadRequestException(message);
            default:
                throw new InternalServerErrorException();
        }
    }
}
