package com.softjourn;


import com.softjourn.executive.Credit;
import com.softjourn.executive.Executive;
import com.softjourn.executive.Vend;
import com.softjourn.keyboard.KeyboardEmulator;
import com.softjourn.machine.Machine;
import com.softjourn.sellcontrol.SellController;
import com.sun.net.httpserver.HttpExchange;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class RequestProcessor implements Runnable {

    private RequestsHolder holder;

    private Machine machine;

    private Executive executive;

    private KeyboardEmulator keyboardEmulator;

    private SellController listener;


    public RequestProcessor(RequestsHolder holder, Machine machine, Executive executive, KeyboardEmulator keyboardEmulator, SellController listener) {
        this.holder = holder;
        this.machine = machine;
        this.executive = executive;
        this.keyboardEmulator = keyboardEmulator;
        this.listener = listener;
    }


    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            process();
        }
    }

    private void process() {
        HttpExchange exchange = holder.next();
        synchronized (exchange) {
            String path = exchange.getRequestURI().getPath();
            if (path.startsWith("/service")) {
                processResetEnginesCommand(exchange);
            } else {
                processSellCommand(exchange);
            }
            exchange.notify();
        }
    }

    private void processSellCommand(HttpExchange exchange) {
        try {
            String cell = getCell(exchange);
            log.info("Request for \"" + cell + "\" cell received.");
            log.debug("Sending selected cell to keyboard emulator.");
            keyboardEmulator.sendKey(cell);
            Thread.sleep(9000);
            if (listener.wasSuccessful()) {
                holder.putResult(exchange, Status.SUCCESS);
                log.info("Successful vending.");
            } else {
                holder.putResult(exchange, Status.ERROR);
                log.info("Unsuccessful vending.");
            }
        } catch (InterruptedException e) {
            log.error("Exception during processing request. " + e.getMessage(), e);
            holder.putResult(exchange, Status.ERROR);
        } finally {
            exchange.notify();
        }
    }

    private void processResetEnginesCommand(HttpExchange exchange) {
        try {
            keyboardEmulator.resetEngines();
            holder.putResult(exchange, Status.SUCCESS);
        } catch (InterruptedException e) {
            log.error("Exception during processing request. " + e.getMessage(), e);
            holder.putResult(exchange, Status.ERROR);
        } finally {
            exchange.notify();
        }
    }

    private Status sell(int timeout) {
        try {
            int currentSeconds = (int) (System.currentTimeMillis() / 1000);
            int endSeconds = currentSeconds + timeout;
            while (currentSeconds < endSeconds) {
                Credit credit = executive.credit(machine);
                if (credit == Credit.VEND_REQUESTED) {
                    log.debug("Vend request from machine received.");
                    log.debug("Sending \"VEND\" command.");
                    Vend vendResult = executive.vend(machine);
                    return vendResult == Vend.SUCCESS ? Status.SUCCESS : Status.ERROR;
                }
                currentSeconds = (int) (System.currentTimeMillis() / 1000);
            }
            return Status.ERROR;
        } catch (IOException | InterruptedException e) {
            return Status.ERROR;
        }

    }

    private String getCell(HttpExchange exchange) {
        return (String) exchange.getAttribute("Cell");
    }

    public enum Status {
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
