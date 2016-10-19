package com.softjourn;

import com.softjourn.executive.Executive;
import com.softjourn.keyboard.KeyboardEmulator;
import com.softjourn.keyboard.RaspberryKeyboardEmulator;
import com.softjourn.machine.Machine;
import com.sun.net.httpserver.HttpServer;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import lombok.Builder;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.mockito.Mockito.mock;

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

        Properties properties = new Properties();
        InputStream propertiesStream = Server.class.getClassLoader().getResourceAsStream("application.properties");
        if (propertiesStream == null) throw new IllegalStateException("Can't find application.properties file");
        properties.load(propertiesStream);

        HttpVendRequestHandler requestHandler = new HttpVendRequestHandler();

        KeyboardEmulator keyboardEmulator = new RaspberryKeyboardEmulator();
        Machine machine = mock(Machine.class);
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
