package com.securebiz;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecureBizApplicationTest {

    @Test
    void applicationClassIsAvailable() {
        assertThat(SecureBizApplication.class).isNotNull();
    }
}
