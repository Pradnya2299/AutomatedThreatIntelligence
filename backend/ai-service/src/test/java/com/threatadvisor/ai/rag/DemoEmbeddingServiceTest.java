package com.threatadvisor.ai.rag;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoEmbeddingServiceTest {

    @Test
    void isDeterministicAnd1536Dimensions() {
        float[] a = DemoEmbeddingService.hashVector("Apache HTTP Server emergency patch");
        float[] b = DemoEmbeddingService.hashVector("Apache HTTP Server emergency patch");
        assertEquals(1536, a.length);
        for (int i = 0; i < a.length; i++) {
            assertEquals(a[i], b[i]);
        }
        String literal = DemoEmbeddingService.toLiteral(a);
        assertTrue(literal.startsWith("["));
        assertTrue(literal.endsWith("]"));
        assertEquals(1535, literal.chars().filter(ch -> ch == ',').count());
    }
}
