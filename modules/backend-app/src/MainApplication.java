import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.ws.rs.core.Application;
import scc.azure.AzureMonolithService;
import scc.azure.config.AzureEnv;
import scc.rest.AuctionResource;
import scc.rest.ControlResource;
import scc.rest.MediaResource;
import scc.rest.UserResource;

public class MainApplication extends Application {
    private final Set<Object> singletons = new HashSet<Object>();
    private final Set<Class<?>> resources = new HashSet<Class<?>>();

    public MainApplication() {
        Logger rootLog = Logger.getLogger("");
        rootLog.setLevel(Level.FINE);
        rootLog.getHandlers()[0].setLevel(Level.FINE);

        resources.add(ControlResource.class);
        resources.add(GenericExceptionMapper.class);

        var backendKind = AzureEnv.getBackendKind();
        switch (backendKind) {
            case AzureEnv.BACKEND_KIND_AZURE:
                var config = AzureEnv.getAzureMonolithConfig();
                var service = new AzureMonolithService(config);
                singletons.add(new MediaResource(service));
                singletons.add(new UserResource(service, service, service));
                singletons.add(new AuctionResource(service));
                break;
        }
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
