package com.softjourn.executive;

import com.softjourn.machine.Machine;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        executive = new Executive();

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
        when(inputStream.read()).thenReturn(0b00000001);

        assertEquals(Status.FREEVEND, executive.status(machine));
        verify(outputStream, times(1)).write(0b00110001);
    }

    @Test
    public void statusFreeVendAuditData() throws Exception {
        when(inputStream.read()).thenReturn(0b01100001);

        assertEquals(Status.FREEVEND, executive.status(machine));
        verify(outputStream, times(1)).write(0b00110001);
    }

    @Test
    public void statusInhibited() throws Exception {
        when(inputStream.read()).thenReturn(0b00000010);
        assertEquals(Status.INHIBITED, executive.status(machine));
        verify(outputStream, times(1)).write(0b00110001);
    }

    @Test
    public void statusInhibitedFreeVend() throws Exception {
        when(inputStream.read()).thenReturn(0b00000011);
        assertEquals(Status.INHIBITED, executive.status(machine));
        verify(outputStream, times(1)).write(0b00110001);
    }

    @Test
    public void cleanAuditTest() throws Exception {
        assertEquals(0b00000001, Executive.cleanAuditData(0b01000001));
        assertEquals(0b00000001, Executive.cleanAuditData(0b11000001));
        assertEquals(0b00000001, Executive.cleanAuditData(0b01010001));
        assertEquals(0b00000001, Executive.cleanAuditData(0b00000001));
        assertEquals(0b00000001, Executive.cleanAuditData(0b11110001));
        assertEquals(0b00000011, Executive.cleanAuditData(0b00000011));
        assertEquals(0b00000011, Executive.cleanAuditData(0b01000011));
        assertEquals(0b00000011, Executive.cleanAuditData(0b11000011));
        assertEquals(0b00000011, Executive.cleanAuditData(0b11100011));
        assertEquals(0b00000011, Executive.cleanAuditData(0b11110011));

    }
}