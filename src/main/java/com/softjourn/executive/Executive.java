package com.softjourn.executive;

import com.softjourn.machine.Machine;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Executive {

    public static final int STATUS_REQUEST = 0b00110001;

    public static final int CREDIT_REQUEST = 0b00110010;

    public static final int VEND_REQUEST = 0b00110011;


    public synchronized Status status(Machine machine) throws IOException {
        writeRequest(machine, STATUS_REQUEST);
        int resp = readResponse(machine);

        int clear = cleanAuditData(resp);

        switch (clear) {
            case 0 : return Status.ACK;
            case 128 : return Status.FREEVEND;
            case 64 :
            case 192 : return Status.INHIBITED;
            default: throw new IllegalStateException();
        }
    }

    public synchronized Credit credit(Machine machine) throws IOException {
        writeRequest(machine, CREDIT_REQUEST);
        int resp = readResponse(machine);

        if (resp == 254) return Credit.NO_VEND_REQUEST;
        else return Credit.VEND_REQUESTED;
    }

    public synchronized void vend(Machine machine) throws IOException, VendingProcessingException {
        writeRequest(machine, VEND_REQUEST);
        int resp = readResponse(machine);
        if (resp != 0) throw new VendingProcessingException();
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
