package com.softjourn;

import com.softjourn.executive.Executive;
import com.softjourn.keyboard.KeyboardEmulator;
import com.softjourn.keyboard.RaspberryKeyboardEmulator;
import com.softjourn.machine.Machine;
import com.softjourn.security.SignSecurityFilter;
import com.softjourn.sellcontrol.SellController;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import lombok.Builder;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
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

    private SignSecurityFilter securityFilter;

    public void start() throws IOException {
        if (server == null) {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.setExecutor(executor);
            HttpContext context = server.createContext("/", requestHandler);
            context.getFilters().add(securityFilter);
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
        private HttpVendRequestHandler requestHandler = new HttpVendRequestHandler(1);
        private Executor executor = Executors.newCachedThreadPool();
        private ExecutorService requestProcessorExecutor = Executors.newSingleThreadExecutor();
    }

    public static void main(String[] args) throws NoSuchPortException, PortInUseException, IOException, CertificateException {

        Properties properties = new Properties();
        InputStream propertiesStream = Server.class.getClassLoader().getResourceAsStream("application.properties");
        if (propertiesStream == null) throw new IllegalStateException("Can't find application.properties file");
        properties.load(propertiesStream);

        HttpVendRequestHandler requestHandler = new HttpVendRequestHandler(1);

        SignSecurityFilter signSecurityFilter = new SignSecurityFilter(readPublicKey());

        KeyboardEmulator keyboardEmulator = new RaspberryKeyboardEmulator();
        Machine machine = mock(Machine.class);
        SellController listener = new SellController(27);

        Executive executive = new Executive();
        RequestProcessor requestProcessor = new RequestProcessor(requestHandler, machine, executive, keyboardEmulator, listener);

        Server.builder()
                .port(7070)
                .requestHandler(requestHandler)
                .requestProcessor(requestProcessor)
                .securityFilter(signSecurityFilter)
                .build()
                .start();
    }

    public static byte[] readPublicKey() throws FileNotFoundException, CertificateException {
        InputStream fin = Server.class.getClassLoader().getResourceAsStream("security.cert");
        CertificateFactory f = CertificateFactory.getInstance("X.509");
        X509Certificate certificate = (X509Certificate)f.generateCertificate(fin);
        PublicKey pk = certificate.getPublicKey();
        return pk.getEncoded();
    }
}
