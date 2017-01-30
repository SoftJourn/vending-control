package com.softjourn.sellcontrol;

import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

import java.util.concurrent.CountDownLatch;

import static com.pi4j.io.gpio.PinEdge.RISING;

public class SellControlListener {

    private GpioPinDigitalOutput vccPin;
    private GpioPinDigitalInput signalPin;

    public SellControlListener(int vccPinNumber, int signalPinNumber) {
        GpioController gpio = GpioFactory.getInstance();
        vccPin = gpio.provisionDigitalOutputPin(RaspiPin.getPinByAddress(vccPinNumber), PinState.LOW);
        signalPin = gpio.provisionDigitalInputPin(RaspiPin.getPinByAddress(signalPinNumber));
    }

    public void startListen(CountDownLatch monitor) {
        signalPin.addListener((GpioPinListenerDigital) (event -> {
            if (event.getEdge() == RISING) {
                monitor.countDown();
            }
        }));
        vccPin.high();
    }


    public void stopListen() {
        signalPin.removeAllListeners();
        vccPin.low();
    }
}
