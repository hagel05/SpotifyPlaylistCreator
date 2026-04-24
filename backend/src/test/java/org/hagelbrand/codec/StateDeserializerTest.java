package org.hagelbrand.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.hagelbrand.data.SetlistSearchResponse.State;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StateDeserializerTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        SimpleModule module = new SimpleModule();
        module.addDeserializer(State.class, new StateDeserializer());

        objectMapper.registerModule(module);
    }

    @Test
    void deserializesStateFromString() throws Exception {
        String json = """
            {
              "state": "Nevada"
            }
            """;

        Wrapper wrapper = objectMapper.readValue(json, Wrapper.class);

        State state = wrapper.state();

        assertThat(state).isNotNull();
        assertThat(state.name()).isEqualTo("Nevada");
        assertThat(state.code()).isNull();
    }

    @Test
    void deserializesStateFromObject() throws Exception {
        String json = """
            {
              "state": {
                "name": "California",
                "code": "CA"
              }
            }
            """;

        Wrapper wrapper = objectMapper.readValue(json, Wrapper.class);

        State state = wrapper.state();

        assertThat(state).isNotNull();
        assertThat(state.name()).isEqualTo("California");
        assertThat(state.code()).isEqualTo("CA");
    }

    @Test
    void returnsNullForUnexpectedStateShape() throws Exception {
        String json = """
            {
              "state": 123
            }
            """;

        Wrapper wrapper = objectMapper.readValue(json, Wrapper.class);

        assertThat(wrapper.state()).isNull();
    }

    /**
     * Minimal wrapper to trigger deserialization
     */
    private record Wrapper(State state) {}
}
