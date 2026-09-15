package org.example.style;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

public class StyleTestCase {

    @Test
    public void testPath() {
        String path = CustomStyle.getDefaultStylesheet();
        System.err.println("path: " + path);
        assertThat(path).isNotEmpty();

    }
}
