package scc.resources;

import java.util.Base64;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
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

    public static record CreateUserRequest(
            @JsonProperty(required = true) String nickname,
            @JsonProperty(required = true) String name,
            @JsonProperty(required = true) String password,
            String imageBase64) {
    }

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String createUser(CreateUserRequest request) {
        System.out.println("Received create user request");
        System.out.println(request);

        Optional<byte[]> photo = Optional.empty();
        if (request.imageBase64 != null) {
            photo = Optional.of(Base64.getDecoder().decode(request.imageBase64));
        }

        var result = this.service.createUser(new UserService.CreateUserParams(
                request.nickname(),
                request.name(),
                request.password(),
                photo));

        if (result.isErr())
            this.throwUserError(result.unwrapErr());

        return result.value();
    }

    @DELETE
    @Path("/{" + USER_ID + "}")
    public void deleteUser(@PathParam(USER_ID) String id) {
        System.out.println("Received delete user request for id " + id);
        var result = this.service.deleteUser(id);
        if (result.isErr())
            this.throwUserError(result.unwrapErr());
    }

    public static record UpdateUserRequest(String name, String password, String imageBase64) {
    }

    @PATCH
    @Path("/{" + USER_ID + "}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateUser(@PathParam(USER_ID) String id, UpdateUserRequest request) {
        System.out.println("Received update user request for id " + id);
        System.out.println(request);

        Optional<byte[]> photo = Optional.empty();
        if (request.imageBase64 != null) {
            photo = Optional.of(Base64.getDecoder().decode(request.imageBase64));
        }

        var updateOps = new UserService.UpdateUserOps();
        if (request.name() != null)
            updateOps = updateOps.updateName(request.name());

        if (request.password() != null)
            updateOps = updateOps.updatePassword(request.password());

        if (photo.isPresent())
            updateOps = updateOps.updateImage(photo.get());

        var result = this.service.updateUser(id, updateOps);

        if (result.isErr())
            this.throwUserError(result.unwrapErr());
    }

    private void throwUserError(UserService.Error error) {
        switch (error) {
            case USER_NOT_FOUND:
                throw new NotFoundException();
            case USER_ALREADY_EXISTS:
                throw new WebApplicationException(409);
            default:
                throw new WebApplicationException(500);
        }
    }
}
