package com.softjourn.executive;

import com.softjourn.machine.Machine;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Executive {

    public static final int STATUS_REQUEST = 0b00110001;

    public static final int CREDIT_REQUEST = 0b00110010;


    public synchronized Status status(Machine machine) throws IOException {
        writeRequest(machine, STATUS_REQUEST);
        int resp = readResponce(machine);

        int clear = cleanAuditData(resp);

        switch (clear) {
            case 0 : return Status.ACK;
            case 1 : return Status.FREEVEND;
            case 2 :
            case 3 : return Status.INHIBITED;
            default: throw new IllegalStateException();
        }
    }

    public synchronized Credit credit(Machine machine) throws IOException {
        writeRequest(machine, CREDIT_REQUEST);
        int resp = readResponce(machine);

        if (resp == 254) return Credit.NO_VEND_REQUEST;
        else return Credit.VEND_REQUESTED;
    }

    private void writeRequest(Machine machine, int request) throws IOException {
        OutputStream outputStream = machine.getOutputStream();
        outputStream.write(request);
    }

    private int readResponce(Machine machine) throws IOException {
        InputStream inputStream = machine.getInputStream();
        return inputStream.read();
    }

    static int cleanAuditData(int val) {
        return (val << 28) >>> 28;
    }
}
