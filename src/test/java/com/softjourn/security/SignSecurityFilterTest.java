package com.softjourn.security;

import org.junit.Test;

import java.security.AccessControlException;
import java.security.Signature;
import java.time.Instant;
import java.util.HashSet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;


public class SignSecurityFilterTest {

    HashSet<Instant> used = new HashSet<>();

    SignSecurityFilter filter = new SignSecurityFilter(used, mock(Signature.class));

    @Test
    public void verifyTime() throws Exception {
        String time = Instant.now().toEpochMilli() + "";
        Thread.sleep(500);

        assertTrue(filter.verifyTime(time));
    }

    @Test(expected = AccessControlException.class)
    public void verifyTimeTooLong() throws Exception {
        String time = Instant.now().plusSeconds(10).toEpochMilli() + "";
        Thread.sleep(500);

        assertFalse(filter.verifyTime(time));
    }

    @Test(expected = AccessControlException.class)
    public void verifyTimeTwice() throws Exception {
        String time = Instant.now().toEpochMilli() + "";
        Thread.sleep(500);
        assertTrue(filter.verifyTime(time));
        Thread.sleep(500);
        assertFalse(filter.verifyTime(time));
    }

    @Test(expected = AccessControlException.class)
    public void verifyTimeWrongRequest() throws Exception {
        String time = Instant.now().toEpochMilli() + "tt";
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
            time = instant.toEpochMilli() + "";
            assertTrue(filter.verifyTime(time));
        }
        assertTrue(used.contains(instant));
        assertFalse(used.contains(first));


    }



}