package com.softjourn.machine;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Machine implements AutoCloseable {

    private CommPort port;

    public Machine(String portIdentifier) throws NoSuchPortException, PortInUseException {
        port = CommPortIdentifier.getPortIdentifier(portIdentifier).open(System.getProperty("user.name", "root"), 1);
    }

    public InputStream getInputStream() throws IOException {
        return port.getInputStream();
    }

    public OutputStream getOutputStream() throws IOException {
        return port.getOutputStream();
    }

    public void close() throws Exception {
        port.close();
    }

}
