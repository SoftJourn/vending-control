package com.softjourn.sellcontrol;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
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

    public void startListen() {
        signalPin.addListener((GpioPinListenerDigital) (event -> {
            log.debug("VENDING_CONTROL_LISTENER: event on control pin: edge - " + event.getEdge() + ", state - " + event.getState());
        }));
    }


    public void stopListen() {
        signalPin.removeAllListeners();
    }

}
