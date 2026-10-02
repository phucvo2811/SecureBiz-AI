package com.securebiz.copilot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securebiz.copilot.config.LlmProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmClientTest {
    @Test
    void disabledProviderFailsClosedBeforeAnyNetworkRequest() {
        var client = new LlmClient(new LlmProperties(false, "http://127.0.0.1:1", "", "model", "local"),
                new ObjectMapper());

        assertThatThrownBy(() -> client.complete("system", "user"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    void blankModelFailsClosedBeforeAnyNetworkRequest() {
        var client = new LlmClient(new LlmProperties(true, "http://127.0.0.1:1", "", " ", "local"),
                new ObjectMapper());

        assertThatThrownBy(() -> client.complete("system", "user"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no model");
    }
}
