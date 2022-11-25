import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.ws.rs.core.Application;
import scc.kube.KubeServiceFactory;
import scc.kube.config.KubeEnv;
import scc.rest.AuctionResource;
import scc.rest.ControlResource;
import scc.rest.MediaResource;
import scc.rest.UserResource;

public class MainApplication extends Application {
	private static final Logger logger = Logger.getLogger(MainApplication.class.getName());

	private final Set<Object> singletons = new HashSet<Object>();
	private final Set<Class<?>> resources = new HashSet<Class<?>>();

	public MainApplication() throws IOException {
		try {
			assert true == false;
			throw new RuntimeException("Enable assertions with flag '-ea'");
		} catch (AssertionError e) {
		}

		Logger rootLog = Logger.getLogger("");
		rootLog.setLevel(Level.FINE);
		rootLog.getHandlers()[0].setLevel(Level.FINE);

		resources.add(ControlResource.class);
		resources.add(GenericExceptionMapper.class);

		var config = KubeEnv.getKubeConfig();
		var factory = new KubeServiceFactory();
		logger.info("Kube config: " + config);

		var mediaService = factory.createMediaService(config.getMediaConfig());
		singletons.add(new MediaResource(mediaService));

		var auctionService = factory.createAuctionService(config);
		singletons.add(new AuctionResource(auctionService));

		var userService = factory.createUserService(config);
		singletons.add(new UserResource(userService, auctionService, mediaService));

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
