package com.softjourn;

import com.softjourn.executive.Executive;
import com.softjourn.keyboard.KeyboardEmulator;
import com.softjourn.machine.Machine;
import com.sun.net.httpserver.HttpServer;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import lombok.Builder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Builder
public class Server implements AutoCloseable {

    private HttpServer server;

    private int port;

    private HttpVendRequestHandler requestHandler;

    private Executor executor;

    private RequestProcessor requestProcessor;

    private ExecutorService requestProcessorExecutor;

    public void start() throws IOException {
        if (server == null) {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.setExecutor(executor);
            server.createContext("/", requestHandler);
        }
        if (requestProcessor == null) {
            throw new IllegalStateException("RequestProcessor can't be null.");
        }
        requestProcessorExecutor.submit(requestProcessor);
        server.start();
    }

    @Override
    public void close() throws Exception {
        server.stop(0);
        requestProcessorExecutor.shutdownNow();
    }

    public static class ServerBuilder {
        private int port = 7070;
        private HttpVendRequestHandler requestHandler = new HttpVendRequestHandler();
        private Executor executor = Executors.newCachedThreadPool();
        private ExecutorService requestProcessorExecutor = Executors.newSingleThreadExecutor();
    }

    public static void main(String[] args) throws NoSuchPortException, PortInUseException, IOException {
        HttpVendRequestHandler requestHandler = new HttpVendRequestHandler();

        KeyboardEmulator keyboardEmulator = mock(KeyboardEmulator.class);
        InputStream inputStream = mock(InputStream.class);
        OutputStream outputStream = mock(OutputStream.class);
        AtomicInteger lastRequest = new AtomicInteger();
        AtomicInteger vendRequestCounter = new AtomicInteger(0);
        doAnswer(i -> vendRequestCounter.incrementAndGet())
                .when(keyboardEmulator)
                .sendKey(anyString());
        doAnswer(i -> {
            Integer val = (Integer) i.getArguments()[0];
            lastRequest.set(val);
            return null;
        }).when(outputStream).write(anyInt());

        when(inputStream.read()).thenAnswer(i -> {
            int lr = lastRequest.get();
            switch (lr) {
                case Executive.CREDIT_REQUEST : return vendRequestCounter.get() > 0 ? 0 : 254;
                case Executive.STATUS_REQUEST: return vendRequestCounter.get() > 0 ? 128 : 0;
                case Executive.VEND_REQUEST: {
                    Thread.sleep(5000);
                    return vendRequestCounter.getAndDecrement() > 0 ? 0 : 128;
                }
                default: return null;
            }
        });
        Machine machine = mock(Machine.class);
        when(machine.getInputStream()).thenReturn(inputStream);
        when(machine.getOutputStream()).thenReturn(outputStream);
        //Machine machine = new Machine("/dev/ttyS0");

        Executive executive = new Executive();
        RequestProcessor requestProcessor = new RequestProcessor(requestHandler, machine, executive, keyboardEmulator);

        Server.builder()
                .port(7070)
                .requestHandler(requestHandler)
                .requestProcessor(requestProcessor)
                .build()
                .start();
    }
}
