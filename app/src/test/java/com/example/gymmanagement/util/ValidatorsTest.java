package com.example.gymmanagement.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ValidatorsTest {
    @Test
    public void email() {
        assertTrue(Validators.email("a.b+c@example.com"));
        assertFalse(Validators.email("abc@"));
        assertFalse(Validators.email(""));
        assertFalse(Validators.email(null));
    }

    @Test
    public void phone() {
        assertTrue(Validators.phone("0901234567"));
        assertTrue(Validators.phone("+84901234567"));
        assertFalse(Validators.phone("12345"));
        assertFalse(Validators.phone("09012abc67"));
    }

    @Test
    public void passwordAndName() {
        assertTrue(Validators.password("123456"));
        assertFalse(Validators.password("12345"));
        assertTrue(Validators.name("An"));
        assertFalse(Validators.name("A"));
    }

    @Test
    public void cleanNameCollapsesSpaces() {
        assertEquals("Quốc Anh Gym", Validators.cleanName("  Quốc   Anh  Gym "));
    }

    @Test
    public void httpsOrEmpty() {
        assertTrue(Validators.httpsOrEmpty(""));
        assertTrue(Validators.httpsOrEmpty("https://example.com/a.jpg"));
        assertFalse(Validators.httpsOrEmpty("http://example.com/a.jpg"));
        assertFalse(Validators.httpsOrEmpty("https://"));
    }
}
