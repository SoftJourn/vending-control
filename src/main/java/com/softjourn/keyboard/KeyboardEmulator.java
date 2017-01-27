package com.softjourn.keyboard;

public interface KeyboardEmulator {

    void sendKey(String key) throws InterruptedException;

    void resetEngines() throws InterruptedException;

}
