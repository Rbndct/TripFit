package com.itismob.s01.group01.tripfit.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class EventTest {

    @Test
    public void consume_returnsContentOnlyOnce() {
        Event<String> event = new Event<>("boom");

        assertEquals("boom", event.consume());
        assertNull(event.consume());
    }

    @Test
    public void peek_doesNotConsume() {
        Event<Integer> event = new Event<>(7);

        assertEquals(Integer.valueOf(7), event.peek());
        assertEquals(Integer.valueOf(7), event.consume());
    }
}
