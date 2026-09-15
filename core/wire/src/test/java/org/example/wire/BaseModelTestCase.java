package org.example.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class BaseModelTestCase {

    @Test
    public void testBaseModelId() {
        BaseModel base = new BaseModel();

        assertEquals("value", base.getId());
    }
}
