package scc.azure.cache;

import scc.utils.Result;

public interface Cache {

    String get(String key);

    String set(String key, String value);
}
