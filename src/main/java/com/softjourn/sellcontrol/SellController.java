package com.softjourn.sellcontrol;

import com.pi4j.io.gpio.*;

public class SellController {

    private GpioPinDigitalInput signalPin;

    public SellController(int signalPinNumber) {
        GpioController gpio = GpioFactory.getInstance();
        signalPin = gpio.provisionDigitalInputPin(RaspiPin.getPinByAddress(signalPinNumber));
    }

    public boolean wasSuccessful() {
        return signalPin.isHigh();
    }
}
