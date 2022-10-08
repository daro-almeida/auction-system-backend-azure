package scc.srv;

import java.util.HashSet;
import java.util.Set;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import jakarta.ws.rs.core.Application;

public class MainApplication extends Application {
	private final Set<Object> singletons = new HashSet<Object>();
	private final Set<Class<?>> resources = new HashSet<Class<?>>();

	public MainApplication() {
		// -------------------- Media Storage --------------------
		var mediaStorage = new AzureMediaStorage(BuildConstants.AZURE_STORAGE_ACC_CONNECTION_STRING,
				BuildConstants.AZURE_STORAGE_CONTAINER_IMAGES);

		resources.add(ControlResource.class);
		singletons.add(new MediaResource(mediaStorage));
		singletons.add(new UsersResource(mediaStorage));
	}

	@Override
	public Set<Class<?>> getClasses() {
		return resources;
	}

	@Override
	public Set<Object> getSingletons() {
		return singletons;
	}
}
