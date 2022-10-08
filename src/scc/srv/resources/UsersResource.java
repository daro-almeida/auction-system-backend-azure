package scc.srv.resources;

import com.azure.cosmos.models.CosmosPatchOperations;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import scc.data.CosmosDBLayer;
import scc.data.client.User;
import scc.data.database.UserDAO;
import scc.srv.UserCosmosDBLayer;
import scc.srv.mediaStorage.MediaStorage;
import scc.data.JSON.UserJSON;
import scc.utils.Hash;

import java.util.Base64;

import static scc.srv.BuildConstants.*;

/**
 * Resource for managing users.
 */
@Path("/user")
public class UsersResource {

	private final UserCosmosDBLayer db;
	private final MediaStorage mediaStorage;

	public UsersResource(MediaStorage mediaStorage) {
		this.db = new UserCosmosDBLayer();
		this.mediaStorage = mediaStorage;
	}

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String createUser(UserJSON u) {
		if (u.name == null || u.nickname == null || u.password == null || u.imageBase64 == null)
			throw new BadRequestException();

		var photo = Base64.getDecoder().decode(u.imageBase64);
		var photoId = Hash.of(photo);
		mediaStorage.upload(photo);

		var user = new User(u.nickname, u.name, u.password, photoId);
		db.putUser(new UserDAO(user));

		return user.getId();
	}

	@DELETE
	@Path("/{" + USER_ID + "}")
	public void deleteUser(@PathParam(USER_ID) String id) {
		var res = db.delUserById(id);
		if (res.getStatusCode() == 404)
			throw new NotFoundException();
	}

	@PATCH
	@Path("/{" + USER_ID + "}")
	@Consumes(MediaType.APPLICATION_JSON)
	public void updateUser(@PathParam(USER_ID) String id, UserJSON u) {
		var operations = CosmosPatchOperations.create();
		if (u.name != null)
			operations.replace("/name", u.name);
		if (u.nickname != null)
			operations.replace("/nickname", u.nickname);
		if (u.password != null)
			operations.replace("/hashedPwd", Hash.of(u.password));
		if (u.imageBase64 != null) {
			var photo = Base64.getDecoder().decode(u.imageBase64);
			var photoId = Hash.of(photo);
			operations.replace("/photoId", photoId);
			mediaStorage.upload(photo);
		}

		var res = db.updateUser(id, operations);
		if (res.getStatusCode() == 404)
			throw new NotFoundException();
	}
}
