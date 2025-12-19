package org.hagelbrand.codec;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.hagelbrand.data.SetlistSearchResponse;
import org.hagelbrand.data.SetlistSearchResponse.State;

import java.io.IOException;

public class StateDeserializer extends JsonDeserializer<State> {

    @Override
    public State deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {

        JsonNode node = p.getCodec().readTree(p);

        // Case 1: "state": "Nevada"
        if (node.isTextual()) {
            return State.fromString(node.asText());
        }

        // Case 2: "state": { "name": "...", "code": "..." }
        if (node.isObject()) {
            String name = node.path("name").asText(null);
            String code = node.path("code").asText(null);
            return new State(name, code);
        }

        return null;
    }
}
