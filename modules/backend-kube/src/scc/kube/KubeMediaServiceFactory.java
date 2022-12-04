package scc.kube;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import scc.MediaService;
import scc.ServiceFactory;
import scc.kube.config.KubeMediaConfig;

public class KubeMediaServiceFactory implements ServiceFactory<MediaService> {
    private final KubeMediaConfig config;
    private KubeMediaService service;

    public KubeMediaServiceFactory(KubeMediaConfig config) {
        this.config = config;
    }

    @Override
    @WithSpan
    public MediaService createService() {
        return this.getService();
    }

    private synchronized KubeMediaService getService() {
        if (this.service == null) {
            try {
                this.service = new KubeMediaService(this.config);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return this.service;
    }
}
