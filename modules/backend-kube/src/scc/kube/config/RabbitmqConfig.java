package scc.kube.config;

public class RabbitmqConfig {
    public final String host;
    public final int port;

    public RabbitmqConfig(String host, int port) {
        this.host = host;
        this.port = port;
    }
}
