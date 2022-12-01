package scc.kube.config;

public class KubeEnv {

    public static final String KUBE_CACHING_ENABLED = "KUBE_CACHING_ENABLED";

    public static final String KUBE_MEDIA_DATA_DIRECTORY = "KUBE_MEDIA_DATA_DIRECTORY";

    public static final String KUBE_MONGO_CONNECTION_URI = "KUBE_MONGO_CONNECTION_URI";
    public static final String KUBE_MONGO_DATABASE_NAME = "KUBE_MONGO_DATABASE_NAME";
    public static final String KUBE_MONGO_AUCTION_COLLECTION = "KUBE_MONGO_AUCTION_COLLECTION";
    public static final String KUBE_MONGO_BID_COLLECTION = "KUBE_MONGO_BID_COLLECTION";
    public static final String KUBE_MONGO_QUESTION_COLLECTION = "KUBE_MONGO_QUESTION_COLLECTION";
    public static final String KUBE_MONGO_USER_COLLECTION = "KUBE_MONGO_USER_COLLECTION";

    public static final String KUBE_REDIS_URL = "KUBE_REDIS_URL";
    public static final String KUBE_REDIS_PORT = "KUBE_REDIS_PORT";

    public static final String KUBE_RABBITMQ_HOST = "KUBE_RABBITMQ_HOST";
    public static final String KUBE_RABBITMQ_PORT = "KUBE_RABBITMQ_PORT";

    public static KubeConfig getKubeConfig() {
        return new KubeConfig(
                getKubeMediaConfig(),
                getMongoConfig(),
                getRedisConfig(),
                getRabbitmqConfig(),
                getEnableCaching());
    }

    private static KubeMediaConfig getKubeMediaConfig() {
        return new KubeMediaConfig(getEnvVar(KUBE_MEDIA_DATA_DIRECTORY));
    }

    public static MongoConfig getMongoConfig() {
        return new MongoConfig(
                getEnvVar(KUBE_MONGO_CONNECTION_URI),
                getEnvVar(KUBE_MONGO_DATABASE_NAME),
                getEnvVar(KUBE_MONGO_AUCTION_COLLECTION),
                getEnvVar(KUBE_MONGO_BID_COLLECTION),
                getEnvVar(KUBE_MONGO_QUESTION_COLLECTION),
                getEnvVar(KUBE_MONGO_USER_COLLECTION));
    }

    public static RedisConfig getRedisConfig() {
        return new RedisConfig(
                getEnvVar(KUBE_REDIS_URL),
                Integer.parseInt(getEnvVar(KUBE_REDIS_PORT)));
    }

    public static RabbitmqConfig getRabbitmqConfig() {
        return new RabbitmqConfig(
                getEnvVar(KUBE_RABBITMQ_HOST),
                Integer.parseInt(getEnvVar(KUBE_RABBITMQ_PORT)));
    }

    public static boolean getEnableCaching() {
        return Boolean.parseBoolean(getEnvVar(KUBE_CACHING_ENABLED));
    }

    private static String getEnvVar(String name, String defaultValue) {
        var value = System.getenv(name);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    private static String getEnvVar(String name) {
        var value = System.getenv(name);
        if (value == null) {
            throw new RuntimeException("Missing environment variable: " + name);
        }
        return value;
    }
}
