package com.softjourn;


import com.softjourn.executive.Credit;
import com.softjourn.executive.Executive;
import com.softjourn.executive.Vend;
import com.softjourn.keyboard.KeyboardEmulator;
import com.softjourn.machine.Machine;
import com.softjourn.sellcontrol.SellController;
import com.sun.net.httpserver.HttpExchange;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Slf4j
public class RequestProcessor implements Runnable {

    public static final int SPACE_TO_LEFT = 200;

    private RequestsHolder holder;

    private Machine machine;

    private Executive executive;

    private KeyboardEmulator keyboardEmulator;

    private SellController listener;

    private WebCamCommander webCamCommander;

    private String histogramProgram;

    private String folderToCheckFreeSpace;

    private String path;

    private String before;

    private String after;

    private Integer height;

    private Integer width;

    public RequestProcessor(RequestsHolder holder, Machine machine, Executive executive, KeyboardEmulator keyboardEmulator, SellController listener, Properties properties) {
        this.holder = holder;
        this.machine = machine;
        this.executive = executive;
        this.keyboardEmulator = keyboardEmulator;
        this.listener = listener;
        this.path = properties.getProperty("webcam.save.path");
        this.before = properties.getProperty("webcam.picture.before");
        this.after = properties.getProperty("webcam.picture.after");
        this.histogramProgram = properties.getProperty("histogram.program");
        this.height = Integer.valueOf(properties.getProperty("webcam.picture.height"));
        this.width = Integer.valueOf(properties.getProperty("webcam.picture.width"));
        this.folderToCheckFreeSpace = properties.getProperty("folder.to.check.free.space");
        webCamCommander = new WebCamCommander();
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

    public void processSellCommand(HttpExchange exchange) {
        try {
            String cell = getCell(exchange);
            log.info("Request for \"" + cell + "\" cell received.");
            log.debug("Sending selected cell to keyboard emulator.");
            String before = this.savePhoto(this.before);
            keyboardEmulator.sendKey(cell);
            if (listener.wasSuccessful(10)) {
                holder.putResult(exchange, Status.SUCCESS);
                log.info("Successful vending.");
                String after = this.savePhoto(this.after);
                if (!before.isEmpty() && !after.isEmpty()) {
                    List<String> histogramComparison = this.executeCommand(new String[]{this.histogramProgram, before, after});
                    this.logHistogramData(histogramComparison, before, after);
                }
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

    public String savePhoto(String name) {
        // check how many free space has left
        File file = new File(this.folderToCheckFreeSpace);
        long freeSpace = file.getFreeSpace() / 1014 / 1024;
        if (freeSpace > SPACE_TO_LEFT) {
            try {
                BufferedImage bufferedImage = webCamCommander.takePhoto(this.width, this.height);
                String datetime = Instant.now().toString();
                String savePath = this.path + "/" + name + "_" + datetime + ".jpg";
                ImageIO.write(bufferedImage, "JPG", new File(savePath));
                return savePath;
            } catch (IOException e) {
                log.error(e.getLocalizedMessage());
                return "";
            }
        } else {
            return "";
        }
    }

    public void logHistogramData(List<String> data, String fileBefore, String fileAfter) {
        Marker marker = MarkerFactory.getMarker("HISTOGRAM");
        log.info(marker, "---------------------------------------------------------------------------------------------");
        log.info(marker, "File before: " + fileBefore);
        log.info(marker, "File after:  " + fileAfter);
        for (String d : data) {
            log.info(marker, d);
        }
        log.info(marker, "---------------------------------------------------------------------------------------------");
    }

    public List<String> executeCommand(String[] command) {
        Runtime runtime = Runtime.getRuntime();
        List<String> result = new ArrayList<>();
        try {
            Process process = runtime.exec(command);
            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(process.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(process.getErrorStream()));
            String line;
            while ((line = stdInput.readLine()) != null) {
                result.add(line);
            }
            while ((line = stdError.readLine()) != null) {
                result.add(line);
            }
        } catch (IOException e) {
            log.error(e.getLocalizedMessage());
        } finally {
            return result;
        }
    }
}
