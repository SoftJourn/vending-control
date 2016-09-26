package com.softjourn.executive;

import com.softjourn.machine.Machine;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ExecutiveTest {

    @Mock
    Machine machine;

    Executive executive;

    @Mock
    InputStream inputStream;

    @Mock
    OutputStream outputStream;

    @Before
    public void setUp() throws Exception {
        executive = new Executive(2, 5);

        when(machine.getInputStream()).thenReturn(inputStream);
        when(machine.getOutputStream()).thenReturn(outputStream);

    }

    @Test
    public void statusACK() throws Exception {
        when(inputStream.read()).thenReturn(0b00000000);

        assertEquals(Status.ACK, executive.status(machine));
        verify(outputStream, times(1)).write(0b00110001);
    }

    @Test
    public void statusFreeVend() throws Exception {
        when(inputStream.read()).thenReturn(0b10000000);

        assertEquals(Status.FREEVEND, executive.status(machine));
        verify(outputStream, times(1)).write(0b00110001);
    }

    @Test(expected = IllegalStateException.class)
    public void statusDataTransmissionErrorOccurredStatusRequest() throws Exception {
        int initSeconds = (int) (System.currentTimeMillis()/1000) + 20; //Return PNAK for next 20 seconds
        when(inputStream.read()).then(i -> {
            int curSeconds = (int) (System.currentTimeMillis() / 1000);
            if (curSeconds < initSeconds) {
                return 255;
            }
            return 0b10000000;
        });

        assertEquals(Status.FREEVEND, executive.status(machine));
        verify(outputStream, times(1)).write(0b00110001);
    }

    @Test
    public void statusDataTransmissionErrorOccurredStatusRequestOneError() throws Exception {
        int times =  1; //Return PNAK for next 1 times
        AtomicInteger count = new AtomicInteger(0);
        when(inputStream.read()).then(i -> {
            if (times < count.getAndAdd(1)) {
                return 255;
            }
            return 0b10000000;
        });

        assertEquals(Status.FREEVEND, executive.status(machine));
        verify(outputStream, times(1)).write(0b00110001);
    }

    @Test
    public void statusFreeVendAuditData() throws Exception {
        when(inputStream.read()).thenReturn(0b10000101);

        assertEquals(Status.FREEVEND, executive.status(machine));
        verify(outputStream, times(1)).write(0b00110001);
    }

    @Test
    public void statusInhibited() throws Exception {
        when(inputStream.read()).thenReturn(0b01000000);
        assertEquals(Status.INHIBITED, executive.status(machine));
        verify(outputStream, times(1)).write(0b00110001);
    }

    @Test
    public void statusInhibitedFreeVend() throws Exception {
        when(inputStream.read()).thenReturn(0b11000000);
        assertEquals(Status.INHIBITED, executive.status(machine));
        verify(outputStream, times(1)).write(0b00110001);
    }

    @Test
    public void creditNoVendRequest() throws Exception {
        when(inputStream.read()).thenReturn(0b11111110);
        assertEquals(Credit.NO_VEND_REQUEST, executive.credit(machine));
        verify(outputStream, times(1)).write(0b00110010);
    }

    @Test
    public void credit5CreditRequest() throws Exception {
        when(inputStream.read()).thenReturn(0b00000101);
        assertEquals(Credit.VEND_REQUESTED, executive.credit(machine));
        verify(outputStream, times(1)).write(0b00110010);
    }

    @Test
    public void successfulVending() throws Exception {
        when(inputStream.read()).thenReturn(0b00000000);
        assertEquals(Vend.SUCCESS, executive.vend(machine));
        verify(outputStream, times(1)).write(0b00110011);
    }

    @Test
    public void unsuccessfulVending() throws Exception {
        when(inputStream.read()).thenReturn(0b10000000);
        assertEquals(Vend.ERROR, executive.vend(machine));
        verify(outputStream, times(1)).write(0b00110011);
    }

    @Test
    public void vendingTimeout() throws Exception {
        int initSeconds = (int) (System.currentTimeMillis()/1000) + 3;
        when(inputStream.read()).then(i -> {
            int curSeconds = (int) (System.currentTimeMillis()/1000);
            if (curSeconds < initSeconds) return -1;
            return 0;
        });
        assertEquals(Vend.TIMEOUT, executive.vend(machine));
        verify(outputStream, times(1)).write(0b00110011);
    }

    @Test
    public void cleanAuditTest() throws Exception {
        assertEquals(0b10000000, Executive.cleanAuditData(0b10000000));
        assertEquals(0b10000000, Executive.cleanAuditData(0b10000001));
        assertEquals(0b10000000, Executive.cleanAuditData(0b10000011));
        assertEquals(0b10000000, Executive.cleanAuditData(0b10000111));
        assertEquals(0b10000000, Executive.cleanAuditData(0b10001111));
        assertEquals(0b11000000, Executive.cleanAuditData(0b11000000));
        assertEquals(0b11000000, Executive.cleanAuditData(0b11000001));
        assertEquals(0b11000000, Executive.cleanAuditData(0b11000011));
        assertEquals(0b11000000, Executive.cleanAuditData(0b11000111));
        assertEquals(0b11000000, Executive.cleanAuditData(0b11001111));

    }

}