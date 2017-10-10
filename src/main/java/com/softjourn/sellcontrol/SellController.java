package com.softjourn.sellcontrol;

import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class SellController {

    private List<GpioPinDigitalInput> signalPins;

    private AtomicBoolean STATE_CHANGED_AFTER_BUYING = new AtomicBoolean(false);

    private AtomicBoolean WAITING_FOR_SELLING = new AtomicBoolean(false);

    private AtomicReference<Instant> lastTymeStateCHanged = new AtomicReference<>(Instant.now());

    public SellController(int... signalPinNumbers) {
        GpioController gpio = GpioFactory.getInstance();
        signalPins = IntStream.of(signalPinNumbers)
                .mapToObj(RaspiPin::getPinByAddress)
                .map(gpio::provisionDigitalInputPin)
                .collect(Collectors.toList());
        startListen();
    }

    public boolean wasSuccessful(int timeoutInSeconds) {
        log.debug("Waiting for sensor signal...");
        STATE_CHANGED_AFTER_BUYING.set(false);
        WAITING_FOR_SELLING.set(true);
        Instant timeLimit = Instant.now().plus(timeoutInSeconds, ChronoUnit.SECONDS);
        while (Instant.now().isBefore(timeLimit)) {
            if (STATE_CHANGED_AFTER_BUYING.get()) {
                return true;
            }
        }
        WAITING_FOR_SELLING.set(false);
        return false;
    }

    private void startListen() {
        signalPins.forEach(sp -> sp.addListener(listener));
    }

    private GpioPinListenerDigital listener = event -> {
        if (WAITING_FOR_SELLING.get()) {
            STATE_CHANGED_AFTER_BUYING.set(true);
        } else if (lastTymeStateCHanged.get().isBefore(Instant.now().minus(1, ChronoUnit.MINUTES))) {
            log.debug("VENDING_CONTROL_LISTENER: event on control pin: edge - " + event.getEdge() + ", state - " + event.getState());
        }
        lastTymeStateCHanged.set(Instant.now());
    };


    public void stopListen() {
        signalPins.forEach(GpioPin::removeAllListeners);
    }

}
