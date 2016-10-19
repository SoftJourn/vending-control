package com.softjourn.keyboard;

import com.pi4j.io.gpio.*;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class RaspberryKeyboardEmulator implements KeyboardEmulator, AutoCloseable {

    private GpioController gpio;

    private Map<Integer, GpioPinDigitalOutput> pins;

    public RaspberryKeyboardEmulator() {
        log.info("Initializing Raspberry GPIO interface ...");
        gpio = GpioFactory.getInstance();
        pins = new HashMap<Integer, GpioPinDigitalOutput>(){{
            put(1, gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, "1", PinState.LOW));
            put(2, gpio.provisionDigitalOutputPin(RaspiPin.GPIO_08, "2", PinState.LOW));
            put(3, gpio.provisionDigitalOutputPin(RaspiPin.GPIO_09, "3", PinState.LOW));
            put(4, gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03, "4", PinState.LOW));
            put(5, gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, "5", PinState.LOW));
            put(6, gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, "6", PinState.LOW));
            put(7, gpio.provisionDigitalOutputPin(RaspiPin.GPIO_14, "7", PinState.LOW));
            put(8, gpio.provisionDigitalOutputPin(RaspiPin.GPIO_12, "8", PinState.LOW));
            put(9, gpio.provisionDigitalOutputPin(RaspiPin.GPIO_13, "9", PinState.LOW));
        }};

        log.info("Initializing done.");
    }

    @Override
    public void sendKey(String key) throws InterruptedException {
        log.info("Input command \"" + key + "\".");
        if (key.matches("^[1-9]{2}$")) {
            GpioPinDigitalOutput rowNumberPin = pins.get(Integer.parseInt(key.substring(0, 1)));
            GpioPinDigitalOutput columnNumberPin = pins.get(Integer.parseInt(key.substring(1)));

            rowNumberPin.pulse(200, true);
            Thread.sleep(500);
            columnNumberPin.pulse(200);
            Thread.sleep(5000);
            log.info("Successful vending.");
        } else {
            log.warn("Wrong key \"" + key + "\".");
        }
    }

    @Override
    public void close() throws Exception {
        log.info("Shutting down Raspberry GPIO interface ...");
        gpio.shutdown();
    }
}
