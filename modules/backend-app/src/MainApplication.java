import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.core.Application;
import scc.azure.AzureEnv;
import scc.azure.AzureMonolithService;
import scc.rest.AuctionResource;
import scc.rest.ControlResource;
import scc.rest.MediaResource;
import scc.rest.UserResource;

public class MainApplication extends Application {
    private final Set<Object> singletons = new HashSet<Object>();
    private final Set<Class<?>> resources = new HashSet<Class<?>>();

    public MainApplication() {
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
