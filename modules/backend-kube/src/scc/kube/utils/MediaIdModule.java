package scc.kube.utils;

import com.fasterxml.jackson.databind.module.SimpleModule;

import scc.MediaNamespace;

public class MediaIdModule extends SimpleModule {

    public MediaIdModule() {

    }

    private static String mediaNamespaceToString(MediaNamespace namespace) {
        return switch (namespace) {
            case User -> "user";
            case Auction -> "auction";
            default -> throw new IllegalArgumentException("Unknown namespace: " + namespace);
        };
    }

    private static MediaNamespace mediaNamespaceFromString(String namespace) {
        return switch (namespace) {
            case "user" -> MediaNamespace.User;
            case "auction" -> MediaNamespace.Auction;
            default -> throw new IllegalArgumentException("Unknown namespace: " + namespace);
        };
    }
}
