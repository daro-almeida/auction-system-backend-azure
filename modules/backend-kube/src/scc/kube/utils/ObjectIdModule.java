package scc.kube.utils;

import java.io.IOException;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class ObjectIdModule extends SimpleModule {

    private static class ObjectIdSerializer extends StdSerializer<ObjectId> {

        protected ObjectIdSerializer() {
            super(ObjectId.class);
        }

        @Override
        public void serialize(ObjectId value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.toHexString());
        }

    }

    private static class ObjectIdDeserializer extends StdDeserializer<ObjectId> {

        protected ObjectIdDeserializer() {
            super(ObjectId.class);
        }

        @Override
        public ObjectId deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
            return new ObjectId(p.getValueAsString());
        }

    }

    public ObjectIdModule() {
        this.addSerializer(ObjectId.class, new ObjectIdSerializer());
        this.addDeserializer(ObjectId.class, new ObjectIdDeserializer());
    }

}
