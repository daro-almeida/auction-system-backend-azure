package scc.srv;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import scc.data.CosmosDBLayer;
import scc.data.User;
import scc.data.UserDAO;
import scc.data.UserJSON;
import scc.utils.Hash;

import java.util.Base64;

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
    public String createUser(UserJSON u) {
        //TODO: upload image to blob and get photoId (hash)
        var photo = Base64.getDecoder().decode(u.imageBase64);
        var photoId = Hash.of(photo);

        //TODO: create User class
        var user = new User(u.nickname, u.name, u.password, photoId);
        //TODO: upload User to db
        db.putUser(new UserDAO(user));

        return user.getId();
    }

    @DELETE
    @Path("/{"+ USER_ID +"}")
    public void deleteUser(@PathParam(USER_ID) String id) {
        var res = db.delUserById(id);

    }

    @PUT
    @Path("/{"+ USER_ID +"}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String updateUser(@PathParam(USER_ID) String id, UserJSON u) {


        //TODO: upload image to blob and get photoId (hash)
        //TODO: create User class
        //TODO: upload User to db
        return null; //return userId (?)
    }
}
