package com.softjourn.sellcontrol;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPin;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.softjourn.Server.NOISE_SENSOR;
import static java.lang.String.format;

@Slf4j
public class SellController {

    private List<GpioPinDigitalInput> signalPins;

    private AtomicBoolean STATE_CHANGED_AFTER_BUYING = new AtomicBoolean(false);

    private AtomicBoolean WAITING_FOR_SELLING = new AtomicBoolean(false);

    private AtomicReference<Instant> lastTimeStateCHanged = new AtomicReference<>(Instant.now());

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
        log.debug(format("STATE_CHANGED_AFTER_BUYING: %b", STATE_CHANGED_AFTER_BUYING.get()));
        WAITING_FOR_SELLING.set(true);
        log.debug(format("WAITING_FOR_SELLING: %b", WAITING_FOR_SELLING.get()));
        Instant timeLimit = Instant.now().plus(timeoutInSeconds, ChronoUnit.SECONDS);
        while (Instant.now().isBefore(timeLimit)) {
            if (STATE_CHANGED_AFTER_BUYING.get()) {
                log.debug(format("STATE_CHANGED_AFTER_BUYING: %b", STATE_CHANGED_AFTER_BUYING.get()));
                return true;
            }
        }
        WAITING_FOR_SELLING.set(false);
        return true;
    }

    private void startListen() {
        signalPins.forEach(sp -> sp.addListener(listener));
    }

    private GpioPinListenerDigital listener = event -> {
        int address = event.getPin().getPin().getAddress();
        log.info(format("Pin address: %d", address));
        if (address == NOISE_SENSOR) {
            log.info("Data from noise sensor");
        } else {
            log.info("Data from optical sensor");
        }
        if (WAITING_FOR_SELLING.get()) {
            STATE_CHANGED_AFTER_BUYING.set(true);
        } else if (lastTimeStateCHanged.get().isBefore(Instant.now().minus(1, ChronoUnit.MINUTES))) {
            log.debug("VENDING_CONTROL_LISTENER: event on control pin: edge - " + event.getEdge() + ", state - " + event.getState());
        }
        lastTimeStateCHanged.set(Instant.now());
        log.debug(format("lastTimeStateCHanged: %b", lastTimeStateCHanged.toString()));
    };


    public void stopListen() {
        signalPins.forEach(GpioPin::removeAllListeners);
    }

}
