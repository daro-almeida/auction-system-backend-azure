package scc.data.database.CosmosDB;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.*;
import com.azure.cosmos.util.CosmosPagedIterable;
import scc.data.database.CosmosDB.CosmosDBLayer;
import scc.data.database.UserDAO;

import java.util.Optional;

/**
 * Access to Users database
 */
public class UserCosmosDBLayer extends CosmosDBLayer {

	private static final String USERS_CONTAINER_NAME = "users";

	private final CosmosContainer users;

	/**
	 * Default constructor
	 */
	public UserCosmosDBLayer() {
		super();
		users = db.getContainer(USERS_CONTAINER_NAME);
	}

	/**
	 * Removes the user with given id from the database
	 * @param id Identifier of the user
	 * @return Response of the deletion of the user in the database
	 */
	public CosmosItemResponse<Object> delUserById(String id) {
		PartitionKey key = new PartitionKey(id);
		return users.deleteItem(id, key, new CosmosItemRequestOptions());
	}

	/**
	 * Removes the user with same attributes as the given object
	 * @param user Object which identifies a user
	 * @return Response of the deletion of the user in the database
	 */
	public CosmosItemResponse<Object> delUser(UserDAO user) {
		return users.deleteItem(user, new CosmosItemRequestOptions());
	}

	/**
	 * Inserts the user with given parameters present in the object
	 * @param user Object with the required parameters to represent a user
	 * @return Response of the creation of the user in the database
	 */
	public CosmosItemResponse<UserDAO> putUser(UserDAO user) {
		return users.createItem(user);
	}

	/**
	 * Gets the user with the same identifier as the one given
	 * @param id Identifier of the user
	 * @return User with same identifier or none if not present in the database
	 */
	public Optional<UserDAO> getUserById(String id) {
		return users.queryItems("SELECT * FROM users WHERE users.id=\"" + id + "\"", new CosmosQueryRequestOptions(),
				UserDAO.class).stream().findFirst();
	}

	/**
	 * Gets all the users present in the database
	 * @return Users saved in the database or none if it's empty
	 */
	public CosmosPagedIterable<UserDAO> getUsers() {
		return users.queryItems("SELECT * FROM users ", new CosmosQueryRequestOptions(), UserDAO.class);
	}

	/**
	 * Updates the user entry with given identifier with new respective values
	 * @param id Identifier of the user
	 * @param operations Updates in the respective values of the user
	 * @return Response of the updates of the user in the database
	 */
	public CosmosItemResponse<UserDAO> updateUser(String id, CosmosPatchOperations operations) {
		return users.patchItem(id, new PartitionKey(id), operations, UserDAO.class);
	}
}
