package com.softjourn;


import com.softjourn.executive.Executive;
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

import static com.softjourn.Status.*;

@Slf4j
public class RequestProcessor implements Runnable {

    private static final String OBJECT_DETECTION_MARKER = "OBJECT_DETECTION";
    private static int TIMEOUT_IN_SECONDS = 10;
    private static final int SUCCESSFUL_STATUS_CODE = 1;
    private static final String WEBCAM_SAVE_PATH = "webcam.save.path";
    private static final String WEBCAM_PICTURE_BEFORE = "webcam.picture.before";
    private static final String WEBCAM_PICTURE_AFTER = "webcam.picture.after";
    private static final String OBJECT_DETECTION_PROGRAM = "object.detection.program";
    private static final String WEBCAM_PICTURE_HEIGHT = "webcam.picture.height";
    private static final String WEBCAM_PICTURE_WIDTH = "webcam.picture.width";
    private static final String DETECTION_DEVICE = "detection.device";
    private static final String CAMERA = "CAMERA";
    private static final String SENSOR = "SENSOR";
    private static final String ENGINE_TIME_OUT = "engine.time.out";

    private RequestsHolder holder;

    private Machine machine;

    private Executive executive;

    private KeyboardEmulator keyboardEmulator;

    private SellController listener;

    private WebCamCommander webCamCommander;

    private String objectDetectionProgram;

    private String path;

    private String before;

    private String after;

    private Integer height;

    private Integer width;

    private String device;

    public RequestProcessor(RequestsHolder holder, Machine machine, Executive executive, KeyboardEmulator keyboardEmulator, SellController listener, Properties properties) {
        this.holder = holder;
        this.machine = machine;
        this.executive = executive;
        this.keyboardEmulator = keyboardEmulator;
        this.listener = listener;
        this.path = properties.getProperty(WEBCAM_SAVE_PATH);
        this.before = properties.getProperty(WEBCAM_PICTURE_BEFORE);
        this.after = properties.getProperty(WEBCAM_PICTURE_AFTER);
        this.objectDetectionProgram = properties.getProperty(OBJECT_DETECTION_PROGRAM);
        this.height = Integer.valueOf(properties.getProperty(WEBCAM_PICTURE_HEIGHT));
        this.width = Integer.valueOf(properties.getProperty(WEBCAM_PICTURE_WIDTH));
        this.device = properties.getProperty(DETECTION_DEVICE);
        TIMEOUT_IN_SECONDS = Integer.parseInt(properties.getProperty(ENGINE_TIME_OUT));
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
            if (device.equals(CAMERA)) {
                doWithCamera(exchange, cell);
            } else if (device.equals(SENSOR)) {
                doWithSensor(exchange, cell);
            } else {
                log.error("Undefined detection device: " + device);
                holder.putResult(exchange, Status.ERROR);
            }
        } catch (InterruptedException e) {
            log.error("Exception during processing request. " + e.getMessage(), e);
            holder.putResult(exchange, Status.ERROR);
        } finally {
            exchange.notify();
        }
    }

    private void doWithSensor(HttpExchange exchange, String cell) throws InterruptedException {
        keyboardEmulator.sendKey(cell);
        if (listener.wasSuccessful(TIMEOUT_IN_SECONDS)) {
            holder.putResult(exchange, Status.SUCCESS);
            log.info("Successful vending.");
        } else {
            holder.putResult(exchange, Status.ERROR);
            log.info("Unsuccessful vending.");
        }
    }

    private void doWithCamera(HttpExchange exchange, String cell) throws InterruptedException {
        webCamCommander = new WebCamCommander();
        String before = this.savePhoto(this.before);
        Thread.sleep(TIMEOUT_IN_SECONDS * 200);
        keyboardEmulator.sendKey(cell);
        Thread.sleep(TIMEOUT_IN_SECONDS * 800);
        String after = this.savePhoto(this.after);
        if (!before.isEmpty() && !after.isEmpty()) {
            ExecutionResponse executionResponse = this.executeCommand(new String[]{this.objectDetectionProgram, before, after});
            this.logData(executionResponse.getAmountOfObjects(), before, after);
            if (executionResponse.getStatus().equals(SUCCESS) && executionResponse.getAmountOfObjects() > 0) {
                holder.putResult(exchange, Status.SUCCESS);
                log.info("Successful vending.");
            } else if (executionResponse.getStatus().equals(ERROR)) {
                holder.putResult(exchange, Status.ERROR);
                log.info("Unsuccessful vending:" + executionResponse.getResponseData());
            }
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
    }

    private void logData(Integer amountOfObjects, String fileBefore, String fileAfter) {
        Marker marker = MarkerFactory.getMarker(OBJECT_DETECTION_MARKER);
        log.info(marker, "---------------------------------------------------------------------------------------------");
        log.info(marker, "File before: " + fileBefore);
        log.info(marker, "File after:  " + fileAfter);
        log.info(marker, "Objects detected:  " + amountOfObjects);
        log.info(marker, "---------------------------------------------------------------------------------------------");
    }

    private ExecutionResponse executeCommand(String[] command) {
        Runtime runtime = Runtime.getRuntime();
        List<String> result = new ArrayList<>();
        String line;
        try {
            Process process = runtime.exec(command);
            process.waitFor();
            try (BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(process.getInputStream()));
                 BufferedReader stdError = new BufferedReader(new
                         InputStreamReader(process.getErrorStream()))) {

                while ((line = stdInput.readLine()) != null) {
                    result.add(line);
                }
                while ((line = stdError.readLine()) != null) {
                    result.add(line);
                }
            }
            return new ExecutionResponse(SUCCESS, process.exitValue(), result);
        } catch (IOException | InterruptedException e) {
            log.error(e.getLocalizedMessage());
            return new ExecutionResponse(ERROR, 0, result);
        }
    }
}
