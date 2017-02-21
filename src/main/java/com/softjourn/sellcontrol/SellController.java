package com.softjourn.sellcontrol;

import com.pi4j.io.gpio.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SellController {

    private GpioPinDigitalInput signalPin;

    public SellController(int signalPinNumber) {
        GpioController gpio = GpioFactory.getInstance();
        signalPin = gpio.provisionDigitalInputPin(RaspiPin.getPinByAddress(signalPinNumber));
    }

    public boolean wasSuccessful() {
        log.debug("Checking sensor state...");
        boolean sensorState = signalPin.isHigh();
        log.debug("Sensor state is: " + (sensorState ? "high" : "low"));
        return true;
    }
}
