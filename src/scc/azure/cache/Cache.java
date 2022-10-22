package scc.azure.cache;

public interface Cache {

    String get(String key);

    String set(String key, String value);
}
