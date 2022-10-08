package scc.srv;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.*;
import com.azure.cosmos.util.CosmosPagedIterable;
import scc.data.CosmosDBLayer;
import scc.data.database.UserDAO;

import java.util.Optional;

public class UserCosmosDBLayer extends CosmosDBLayer {

	private static final String USERS_CONTAINER_NAME = "users";

	private CosmosContainer users;

	public UserCosmosDBLayer() {
		super();
		users = db.getContainer(USERS_CONTAINER_NAME);
	}

	public CosmosItemResponse<Object> delUserById(String id) {
		PartitionKey key = new PartitionKey(id);
		return users.deleteItem(id, key, new CosmosItemRequestOptions());
	}

	public CosmosItemResponse<Object> delUser(UserDAO user) {
		return users.deleteItem(user, new CosmosItemRequestOptions());
	}

	public CosmosItemResponse<UserDAO> putUser(UserDAO user) {
		return users.createItem(user);
	}

	public Optional<UserDAO> getUserById(String id) {
		return users.queryItems("SELECT * FROM users WHERE users.id=\"" + id + "\"", new CosmosQueryRequestOptions(),
				UserDAO.class).stream().findFirst();
	}

	public CosmosPagedIterable<UserDAO> getUsers() {
		return users.queryItems("SELECT * FROM users ", new CosmosQueryRequestOptions(), UserDAO.class);
	}

	public CosmosItemResponse<UserDAO> updateUser(String id, CosmosPatchOperations operations) {
		return users.patchItem(id, new PartitionKey(id), operations, UserDAO.class);
	}
}
