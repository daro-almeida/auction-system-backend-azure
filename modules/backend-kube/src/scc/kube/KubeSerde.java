package scc.kube;

import com.fasterxml.jackson.databind.ObjectMapper;

import scc.kube.utils.ObjectIdModule;

public class KubeSerde {
    // https://stackoverflow.com/questions/3907929/should-i-declare-jacksons-objectmapper-as-a-static-field
    private static ObjectMapper objectMapper = createObjectMapper();

    public static String toJson(Object object) {
        try {
            return objectMapper.writer().writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readerFor(clazz).readValue(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T fromJson(byte[] json, Class<T> clazz) {
        try {
            return objectMapper.readerFor(clazz).readValue(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static ObjectMapper createObjectMapper() {
        var objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        objectMapper.registerModule(new ObjectIdModule());
        return objectMapper;
    }
}
