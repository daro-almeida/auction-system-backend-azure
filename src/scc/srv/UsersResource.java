package scc.srv;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import scc.data.User;

/**
 * Resource for managing users.
 */
@Path("/user")
public class UsersResource {

    public UsersResource() {
    }

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public String createUser(String nickname, String name, String password, byte[] photo) {
        //TODO: upload image to blob and get photoId (hash)
        //TODO: create User class
        //TODO: upload User to db
        return null; //return userId (?)
    }

    @DELETE
    @Path("/{id}")
    public void deleteUser(@PathParam("id") String id) {
        //TODO: remove User from db
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public String updateUser(@PathParam("id") String id) {
        //TODO: upload image to blob and get photoId (hash)
        //TODO: create User class
        //TODO: upload User to db
        return null; //return userId (?)
    }
}
