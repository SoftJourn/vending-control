package com.softjourn.executive;

import com.softjourn.machine.Machine;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Executive {

    public static final int STATUS_REQUEST = 0b00110001;

    public synchronized Status status(Machine machine) throws IOException {
        InputStream inputStream = machine.getInputStream();
        OutputStream outputStream = machine.getOutputStream();

        outputStream.write(STATUS_REQUEST);
        int resp = inputStream.read();

        int clear = cleanAuditData(resp);

        switch (clear) {
            case 0 : return Status.ACK;
            case 1 : return Status.FREEVEND;
            case 2 :
            case 3 : return Status.INHIBITED;
            default: throw new IllegalStateException();
        }
    }

    public static int cleanAuditData(int val) {
        return (val << 28) >>> 28;
    }
}
