package org.example.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class BaseModelTestCase {

    @Test
    public void testBaseModelId() {
        BaseModel base = new BaseModel();

        assertThat(base.getId()).isEmpty();
    }
}
