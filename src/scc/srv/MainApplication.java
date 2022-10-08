package scc.srv;

import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.core.Application;
import scc.srv.mediaStorage.AzureMediaStorage;
import scc.srv.resources.AuctionsResource;
import scc.srv.resources.ControlResource;
import scc.srv.resources.MediaResource;
import scc.srv.resources.UsersResource;

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
		singletons.add(new AuctionsResource(media_storage));
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
