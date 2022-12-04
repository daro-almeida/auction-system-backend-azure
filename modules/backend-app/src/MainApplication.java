import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.ws.rs.core.Application;
import scc.kube.KubeServices;
import scc.rest.AuctionResource;
import scc.rest.ControlResource;
import scc.rest.MediaResource;
import scc.rest.UserResource;

public class MainApplication extends Application {
	private static final Logger logger = Logger.getLogger(MainApplication.class.getName());

	private final Set<Object> singletons = new HashSet<Object>();
	private final Set<Class<?>> resources = new HashSet<Class<?>>();

	public MainApplication() throws IOException, TimeoutException {
		try {
			assert true == false;
			throw new RuntimeException("Enable assertions with flag '-ea'");
		} catch (AssertionError e) {
		}

		Logger rootLog = Logger.getLogger("");
		rootLog.setLevel(Level.FINE);
		rootLog.getHandlers()[0].setLevel(Level.FINE);

		resources.add(ControlResource.class);
		resources.add(ServiceExceptionMapper.class);
		resources.add(GenericExceptionMapper.class);

		var services = new KubeServices();

		var mediaService = services.getMediaServiceFactory();
		singletons.add(new MediaResource(mediaService));

		var auctionService = services.getAuctionServiceFactory();
		singletons.add(new AuctionResource(auctionService));

		var userService = services.getUserServiceFactory();
		singletons.add(new UserResource(userService, auctionService));

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
