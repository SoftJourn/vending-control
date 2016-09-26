package com.softjourn.keyboard;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;

public class KeyboardEmulator {

    private CommPort port;

    public KeyboardEmulator(String portIdentifier) throws NoSuchPortException, PortInUseException {
        port = CommPortIdentifier.getPortIdentifier(portIdentifier).open(System.getProperty("user.name", "root"), 1);
    }

    public void sendKey(String key) {

    }
}
