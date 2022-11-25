import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.ws.rs.core.Application;
import scc.kube.KubeMediaService;
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
		Logger rootLog = Logger.getLogger("");
		rootLog.setLevel(Level.FINE);
		rootLog.getHandlers()[0].setLevel(Level.FINE);

		resources.add(ControlResource.class);
		resources.add(GenericExceptionMapper.class);

		var config = KubeEnv.getKubeConfig();
		logger.info("Kube config: " + config);

		var mediaService = new KubeMediaService(config.getMediaConfig());
		singletons.add(new MediaResource(mediaService));
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
