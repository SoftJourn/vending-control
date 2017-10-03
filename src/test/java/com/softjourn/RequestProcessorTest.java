package com.softjourn;

import com.softjourn.executive.Executive;
import com.softjourn.keyboard.KeyboardEmulator;
import com.softjourn.machine.Machine;
import com.softjourn.sellcontrol.SellController;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class RequestProcessorTest {

    @Mock
    SellController sellController;

    @Mock
    RequestsHolder requestsHolder;

    @Mock
    Machine machine;

    @Mock
    Executive executive;

    @Mock
    KeyboardEmulator emulator;

    Properties properties;

    RequestProcessor requestProcessor;

    @Before
    public void setUp() throws IOException {
        properties = new Properties();
        InputStream propertiesStream = Server.class.getClassLoader().getResourceAsStream("application.properties");
        if (propertiesStream == null) throw new IllegalStateException("Can't find application.properties file");
        properties.load(propertiesStream);
        requestProcessor = new RequestProcessor(requestsHolder, machine, executive, emulator, sellController, properties);
    }

    @Test
    public void webCamProperties() {
        assertEquals(5, properties.stringPropertyNames()
                .stream()
                .filter(s -> s.matches("(.*)^webcam(.*)"))
                .collect(Collectors.toList()).size());
    }

    @Test
    public void histogramProperty() {
        assertEquals(1, properties.stringPropertyNames()
                .stream()
                .filter(s -> s.matches("(.*)^histogram(.*)"))
                .collect(Collectors.toList()).size());
    }

//    Use this test only in case you want to modify histogram statistic
//    @Test
//    public void callHistogramProgramTest() {
//        String before = requestProcessor.savePhoto("before");
//        String after = requestProcessor.savePhoto("after");
//        requestProcessor.logHistogramData(requestProcessor.executeCommand(new String[]{"/home/kraytsman/workspace/compare_histograms/compare_histograms", before, after}), before, after);
//    }

}
