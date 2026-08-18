package com.threatadvisor.api.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonSupportTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void readsTextFieldFromReasons() {
        assertEquals("Risk is CRITICAL", JsonSupport.textField(mapper, "{\"text\":\"Risk is CRITICAL\"}", "text"));
    }

    @Test
    void demoModelDetectsLabeledMocks() {
        assertTrue(JsonSupport.demoModel("demo-deterministic"));
        assertFalse(JsonSupport.demoModel("gpt-4o-mini"));
    }

    @Test
    void stringListParsesJsonArray() {
        assertEquals(2, JsonSupport.stringList(mapper, "[\"backup\",\"ticket\"]").size());
    }
}
