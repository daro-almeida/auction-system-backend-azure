package scc.srv;

import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.core.Application;

public class MainApplication extends Application {
	private Set<Object> singletons = new HashSet<Object>();
	private Set<Class<?>> resources = new HashSet<Class<?>>();

	public MainApplication() {
		// -------------------- Media Storage --------------------
		var media_storage = new AzureMediaStorage(BuildConstants.AZURE_STORAGE_ACC_CONNECTION_STRING,
				BuildConstants.AZURE_STORAGE_CONTAINER_IMAGES);
		// var users_media_storage = new
		// AzureMediaStorage(BuildConstants.AZURE_STORAGE_ACC_CONNECTION_STRING,
		// BuildConstants.AZURE_STORAGE_CONTAINER_IMAGES);

		resources.add(ControlResource.class);
		singletons.add(new MediaResource(media_storage));
		singletons.add(new UsersResource());
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
