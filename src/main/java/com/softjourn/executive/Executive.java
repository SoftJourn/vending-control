package com.softjourn.executive;

import com.softjourn.machine.Machine;

public class Executive {

    public synchronized Status status(Machine machine) {
        return Status.ACK;
    }
}
