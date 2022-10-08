package scc.srv;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import scc.data.CosmosDBLayer;
import scc.data.User;
import scc.data.UserParamsJSON;

import static scc.srv.BuildConstants.*;

/**
 * Resource for managing users.
 */
@Path("/user")
public class UsersResource {

    private final CosmosDBLayer db;

    public UsersResource() {
        db = CosmosDBLayer.getInstance();
    }

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String createUser(UserParamsJSON u) {
        System.out.println(u);
        //TODO: upload image to blob and get photoId (hash)
        //TODO: create User class
        //TODO: upload User to db
        //db.putUser();
        return "1"; //return userId (?)
    }

    @DELETE
    @Path("/{"+ USER_ID +"}")
    public void deleteUser(@PathParam(USER_ID) String id) {
        db.delUserById(id);
    }

    @PUT
    @Path("/{"+ USER_ID +"}")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public String updateUser(@PathParam(USER_ID) String id) {
        //TODO: upload image to blob and get photoId (hash)
        //TODO: create User class
        //TODO: upload User to db
        return null; //return userId (?)
    }
}
