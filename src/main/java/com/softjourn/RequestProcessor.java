package com.softjourn;


import com.softjourn.executive.Credit;
import com.softjourn.executive.Executive;
import com.softjourn.executive.Vend;
import com.softjourn.keyboard.KeyboardEmulator;
import com.softjourn.machine.Machine;
import com.sun.net.httpserver.HttpExchange;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Slf4j
public class RequestProcessor implements Runnable {

    private AtomicBoolean stop = new AtomicBoolean(false);

    private RequestsHolder holder;

    private Machine machine;

    private Executive executive;

    private KeyboardEmulator keyboardEmulator;

    private Map<String, Consumer<HttpExchange>> routes = new HashMap<>();

    public RequestProcessor(RequestsHolder holder, Machine machine, Executive executive, KeyboardEmulator keyboardEmulator) {
        this.holder = holder;
        this.machine = machine;
        this.executive = executive;
        this.keyboardEmulator = keyboardEmulator;
    }

    public RequestProcessor setPathController(String path, Consumer<HttpExchange> controller) {
        routes.put(path, controller);
        return this;
    }

    @Override
    public void run() {
        while (! Thread.currentThread().isInterrupted()) {
            processSellCommand();
        }
    }

    private void process() {
        try {
            HttpExchange exchange = holder.next();
            synchronized (exchange) {
                String path = exchange.getRequestURI().getPath();
                getPathController(path)
                        .orElseThrow(() -> new IOException("There is no controller for " + path + "."))
                        .accept(exchange);
                exchange.notify();
            }
        } catch (IOException e) {
            log.error("Exception during processing request. " + e.getMessage(), e);
        }
    }

    private void processSellCommand() {
        try {
            HttpExchange exchange = holder.next();
            synchronized (exchange) {
                String cell = getCell(exchange);
                log.info("Request for \"" + cell + "\" cell received.");
                log.debug("Sending selected cell to keyboard emulator.");
                keyboardEmulator.sendKey(cell);

                holder.putResult(exchange, Status.SUCCESS);
                exchange.notify();
            }
        } catch (IOException | InterruptedException e) {
            log.error("Exception during processing request. " + e.getMessage(), e);
        }
    }

    private void processResetEnginesCommand() {

    }

    private Optional<Consumer<HttpExchange>> getPathController(String path) {
        return Optional.of(httpExchange -> System.out.println());
    }

    private Status sell(int timeout) {
        try {
            int currentSeconds = (int) (System.currentTimeMillis()/1000);
            int endSeconds = currentSeconds + timeout;
            while (currentSeconds < endSeconds) {
                Credit credit = executive.credit(machine);
                if (credit == Credit.VEND_REQUESTED) {
                    log.debug("Vend request from machine received.");
                    log.debug("Sending \"VEND\" command.");
                    Vend vendResult = executive.vend(machine);
                    return vendResult == Vend.SUCCESS ? Status.SUCCESS : Status.ERROR;
                }
                currentSeconds = (int) (System.currentTimeMillis()/1000);
            }
            return Status.ERROR;
        } catch (IOException | InterruptedException e) {
            return Status.ERROR;
        }

    }

    private String getCell(HttpExchange exchange) throws IOException {
        return (String) exchange.getAttribute("Cell");
    }

    public enum  Status {
        SUCCESS(200), ERROR(500);

        private int code;

        public int code() {
            return code;
        }

        Status(int code) {
            this.code = code;
        }
    }
}
