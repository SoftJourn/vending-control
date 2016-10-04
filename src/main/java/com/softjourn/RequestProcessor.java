package com.softjourn;


import com.softjourn.executive.Credit;
import com.softjourn.executive.Executive;
import com.softjourn.executive.Vend;
import com.softjourn.keyboard.KeyboardEmulator;
import com.softjourn.machine.Machine;
import com.sun.net.httpserver.HttpExchange;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.IOException;

@Slf4j
public class RequestProcessor implements Runnable {

    public static final int KEYBOARD_WAITING_TIMEOUT_SECONDS = 5;

    private RequestsHolder holder;

    private Machine machine;

    private Executive executive;

    private KeyboardEmulator keyboardEmulator;

    public RequestProcessor(RequestsHolder holder, Machine machine, Executive executive, KeyboardEmulator keyboardEmulator) {
        this.holder = holder;
        this.machine = machine;
        this.executive = executive;
        this.keyboardEmulator = keyboardEmulator;
    }

    @Override
    public void run() {
        while (! Thread.currentThread().isInterrupted()) {
            process();
        }
    }

    private void process() {
        try {
            HttpExchange exchange = holder.next();
            synchronized (exchange) {
                String cell = getCell(exchange);
                log.info("Request for \"" + cell + "\" cell received.");
                log.debug("Sending selected cell to keyboard emulator.");
                keyboardEmulator.sendKey(cell);
                Status result = sell(KEYBOARD_WAITING_TIMEOUT_SECONDS);
                log.info("Vending result - " + result);
                holder.putResult(exchange, result);
                exchange.notify();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        return IOUtils.toString(exchange.getRequestBody(), "utf8");
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
