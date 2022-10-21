package scc.azure.cache;

public class NoOpCache implements Cache {

    @Override
    public String get(String key) {
        return null;
    }

    @Override
    public String set(String key, String value) {
        return null;
    }
}
