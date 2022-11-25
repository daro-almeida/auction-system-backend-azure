package scc.kube.config;

public class RedisConfig {
    public final String url;
    public final int port;

    public RedisConfig(String url, int port) {
        this.url = url;
        this.port = port;
    }

    @Override
    public String toString() {
        return "RedisConfig [url=" + url + ", port=" + port + "]";
    }

}