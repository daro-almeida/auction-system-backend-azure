package scc.azure.config;

public class RedisConfig {
    public final String key;
    public final String url;

    public RedisConfig(String key, String url) {
        this.key = key;
        this.url = url;
    }
}