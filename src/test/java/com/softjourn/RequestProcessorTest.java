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
import static org.junit.Assert.assertTrue;

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
                .filter(s -> s.matches("(.*)^object.detection(.*)"))
                .collect(Collectors.toList()).size());
    }

    @Test
    public void timeOutProperty() {
        assertEquals(1, properties.stringPropertyNames()
                .stream()
                .filter(s -> s.matches("(.*)^engine.time.out(.*)"))
                .collect(Collectors.toList()).size());
    }

    @Test
    public void detectionProperty() {
        assertEquals(1, properties.stringPropertyNames()
                .stream()
                .filter(s -> s.matches("(.*)^detection.device(.*)"))
                .collect(Collectors.toList()).size());
        String device = properties.getProperty("detection.device");
        assertTrue(device.equals("SENSOR") || device.equals("CAMERA"));
    }

//    Use this test only in case you want to modify histogram statistic
//    @Test
//    public void callHistogramProgramTest() {
//        String before = requestProcessor.savePhoto("before");
//        String after = requestProcessor.savePhoto("after");
//        requestProcessor.logData(requestProcessor.executeCommand(new String[]{"/home/kraytsman/workspace/compare_histograms/object_detection", before, after}).getResponseData(), before, after);
//    }

}
