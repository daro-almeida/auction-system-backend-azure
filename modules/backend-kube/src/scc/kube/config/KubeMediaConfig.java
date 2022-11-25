package scc.kube.config;

public class KubeMediaConfig {
    public final String dataDirectory;

    public KubeMediaConfig(String dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    @Override
    public String toString() {
        return "KubeMediaConfig [dataDirectory=" + dataDirectory + "]";
    }
}
