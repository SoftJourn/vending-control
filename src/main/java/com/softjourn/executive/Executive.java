package com.softjourn.executive;

import com.softjourn.machine.Machine;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Executive {

    public static final int STATUS_REQUEST = 0b00110001;

    public static final int CREDIT_REQUEST = 0b00110010;

    public static final int VEND_REQUEST = 0b00110011;

    public static final int DEFAULT_VEND_TIMEOUT_SECONDS = 60;
    private static final int DEFAULT_TRY_COUNT = 5;

    private int timeoutSeconds;
    private int tryCount;

    public Executive() {
        this(DEFAULT_VEND_TIMEOUT_SECONDS, DEFAULT_TRY_COUNT);
    }

    /**
     * Timeout to wait for vend command response
     * Default is 60 seconds. It's recommended in Executive protocol documentation
     * to use at least 60 seconds.
     * Try count to send request on PNAK response. Vending machine send PNAK response when it receive "wrong request"
     * i.e. when data was corrupted in transfer layer and parity bit doesn't match.
     * Default try count is set to 5
     *
     * @param vendingTimeoutSeconds timeout to wait for response.
     * @param tryCount count to try send data on PNAK response
     */
    public Executive(int vendingTimeoutSeconds, int tryCount) {
        this.timeoutSeconds = vendingTimeoutSeconds;
        this.tryCount = tryCount;
    }

    public synchronized Status status(Machine machine) throws IOException {
        return status(machine, tryCount);
    }

    private Status status(Machine machine, int tryCount) throws IOException {
        writeRequest(machine, STATUS_REQUEST);
        int resp = readResponse(machine);

        if (resp == 255) {
            if (tryCount > 0) return status(machine, tryCount - 1);
            else throw new IllegalStateException("Error during transmitting data. Vending machine respond with PNAK.");
        }

        int clear = cleanAuditData(resp);

        switch (clear) {
            case 0 : return Status.ACK;
            case 128 : return Status.FREEVEND;
            case 64 :
            case 192 : return Status.INHIBITED;
            default: throw new IllegalStateException("Illegal response from vending machine.");
        }
    }

    public synchronized Credit credit(Machine machine) throws IOException {
        return credit(machine, tryCount);
    }

    public Credit credit(Machine machine, int tryCount) throws IOException {
        writeRequest(machine, CREDIT_REQUEST);
        int resp = readResponse(machine);

        if (resp == 255) {
            if (tryCount > 0) return credit(machine, tryCount - 1);
            else throw new IllegalStateException("Error during transmitting data. Vending machine respond with PNAK.");
        }

        if (resp == 254) return Credit.NO_VEND_REQUEST;
        else return Credit.VEND_REQUESTED;
    }

    public synchronized Vend vend(Machine machine) throws IOException, InterruptedException {
        return vend(machine, tryCount);
    }

    private Vend vend(Machine machine, int tryCount) throws IOException, InterruptedException {
        writeRequest(machine, VEND_REQUEST);

        int currentTime = (int) (System.currentTimeMillis()/1000);
        int endTime = currentTime + timeoutSeconds;

        int resp = -1;
        while (currentTime < endTime) {
            resp = readResponse(machine);

            if (resp == 255) {
                if (tryCount > 0) return vend(machine, tryCount - 1);
                else throw new IllegalStateException("Error during transmitting data. Vending machine respond with PNAK.");
            }

            if (resp != -1) break;
            Thread.sleep(500);
            currentTime = (int) (System.currentTimeMillis()/1000);
        }
        return  resp == 0 ? Vend.SUCCESS : resp == -1 ? Vend.TIMEOUT : Vend.ERROR;
    }

    private void writeRequest(Machine machine, int request) throws IOException {
        OutputStream outputStream = machine.getOutputStream();
        outputStream.write(request);
    }

    private int readResponse(Machine machine) throws IOException {
        InputStream inputStream = machine.getInputStream();
        return inputStream.read();
    }

    static int cleanAuditData(int val) {
        return (val >>> 4) << 4;
    }
}
