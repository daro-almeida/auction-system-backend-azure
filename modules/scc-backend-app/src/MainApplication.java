import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.core.Application;
import scc.azure.AzureMonolithService;
import scc.memory.MemoryAuctionService;
import scc.memory.MemoryMediaService;
import scc.memory.MemoryUserService;
import scc.resources.AuctionResource;
import scc.resources.ControlResource;
import scc.resources.MediaResource;
import scc.resources.UserResource;
import scc.utils.SccEnv;

public class MainApplication extends Application {
    private final Set<Object> singletons = new HashSet<Object>();
    private final Set<Class<?>> resources = new HashSet<Class<?>>();

    public MainApplication() {
        resources.add(ControlResource.class);
        resources.add(GenericExceptionMapper.class);

        var backendKind = SccEnv.getBackendKind();
        switch (backendKind) {
            case SccEnv.BACKEND_KIND_MEM:
                System.out.println("Using memory backend");
                var mediaService = new MemoryMediaService();
                var userService = new MemoryUserService(mediaService);
                var auctionService = new MemoryAuctionService(userService, mediaService);
                singletons.add(new MediaResource(mediaService));
                singletons.add(new UserResource(userService, auctionService));
                singletons.add(new AuctionResource(auctionService));
                break;
            case SccEnv.BACKEND_KIND_AZURE:
                var config = SccEnv.getAzureMonolithConfig();
                var service = new AzureMonolithService(config);
                singletons.add(new MediaResource(service));
                singletons.add(new UserResource(service, service));
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
