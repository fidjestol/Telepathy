package com.example.telepathy;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine
 * (host).
 */
public class ExampleUnitTest {


    @Test
    public void addition_isCorrect() {
        assertEquals("Simple arithmetic should work", 4, 2 + 2);
    }

    @Test
    public void string_comparison() {
        String testString = "Hello World";
        assertNotNull("String should not be null", testString);
        assertEquals("Strings should match", "Hello World", testString);
    }

    @Test
    public void boolean_assertion() {
        assertTrue("True should be true", true);
        assertFalse("False should be false", false);
    }
}