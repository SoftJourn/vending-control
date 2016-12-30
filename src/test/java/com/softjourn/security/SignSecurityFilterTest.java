package com.softjourn.security;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.security.AccessControlException;
import java.security.Signature;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class SignSecurityFilterTest {

    private static final String DEFAULT_CEL = "12";

    HashSet<Instant> used = new HashSet<>();

    SignSecurityFilter filter = new SignSecurityFilter(used, mock(Signature.class));

    HttpExchange httpExchange = mock(HttpExchange.class);

    @Before
    public void setUp() throws Exception {
        Headers headers = new Headers();
        headers.put("Authorization", Collections.singletonList(Instant.now().toEpochMilli() + "" + DEFAULT_CEL));

        when(httpExchange.getRequestHeaders()).thenReturn(headers);

        ByteArrayInputStream inputStream = new ByteArrayInputStream("12".getBytes());

        when(httpExchange.getRequestBody()).thenReturn(inputStream);

    }

    @Test
    public void verifyTime() throws Exception {
        String time = Instant.now().toEpochMilli() + "" + DEFAULT_CEL;
        Thread.sleep(500);

        assertTrue(filter.verifyTime(time));
    }

    @Test(expected = AccessControlException.class)
    public void verifyTimeTooLong() throws Exception {
        String time = Instant.now().plusSeconds(10).toEpochMilli() + "" + DEFAULT_CEL;
        Thread.sleep(500);

        assertFalse(filter.verifyTime(time));
    }

    @Test(expected = AccessControlException.class)
    public void verifyTimeTwice() throws Exception {
        String time = Instant.now().toEpochMilli() + "" + DEFAULT_CEL;
        Thread.sleep(500);
        assertTrue(filter.verifyTime(time));
        Thread.sleep(500);
        assertFalse(filter.verifyTime(time));
    }

    @Test(expected = AccessControlException.class)
    public void verifyTimeWrongRequest() throws Exception {
        String time = Instant.now().toEpochMilli() + "ttt";
        Thread.sleep(500);

        assertTrue(filter.verifyTime(time));
    }

    @Test
    public void testRemovingUsed() throws Exception {
        Instant instant = Instant.now();
        String time;
        Instant first = instant;
        for (int i = 0; i < 12; i++) {
            Thread.sleep(1000);
            instant = Instant.now();
            time = instant.toEpochMilli() + "" + DEFAULT_CEL;
            assertTrue(filter.verifyTime(time));
        }
        assertTrue(used.contains(instant));
        assertFalse(used.contains(first));
    }

    @Test
    public void testVerifyCell() {
        filter.verifyCell("1452555585612", "12");
    }

    @Test(expected = AccessControlException.class)
    public void testVerifyCell_wrongCell() {
        filter.verifyCell("1452555585612", "13");
    }

    @Test
    public void testDoFilter() {

    }

}