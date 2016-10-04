package com.softjourn;

import com.softjourn.executive.Executive;
import com.softjourn.keyboard.KeyboardEmulator;
import com.softjourn.machine.Machine;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class Test {

    Server server;

    @Mock
    KeyboardEmulator keyboardEmulator;

    @Mock
    Machine machine;

    @Mock
    InputStream inputStream;

    @Mock
    OutputStream outputStream;

    @Before
    public void setUp() throws Exception {

        when(machine.getInputStream()).thenReturn(inputStream);
        when(machine.getOutputStream()).thenReturn(outputStream);

        AtomicInteger vendRequestCounter = new AtomicInteger(0);

        doAnswer(i -> vendRequestCounter.incrementAndGet())
                .when(keyboardEmulator)
                .sendKey(anyString());

        AtomicInteger lastRequest = new AtomicInteger();

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
                    Thread.sleep(500);
                    return vendRequestCounter.getAndDecrement() > 0 ? 0 : 128;
                }
                default: return null;
            }
        });

        HttpVendRequestHandler requestHandler = new HttpVendRequestHandler();
        Executive executive = new Executive();
        RequestProcessor requestProcessor = new RequestProcessor(requestHandler, machine, executive, keyboardEmulator);

        server = Server.builder()
                .executor(Executors.newCachedThreadPool())
                .port(7070)
                .requestHandler(requestHandler)
                .requestProcessor(requestProcessor)
                .build();
    }

    //Ten requests for 0.5 seconds each should end up with 5 seconds
    @org.junit.Test(timeout = 6000)
    public void plainWorkTest() throws IOException, InterruptedException {
        server.start();

        class Requester implements  Runnable {

            @Override
            public void run() {
                try(CloseableHttpClient httpclient = HttpClients.custom()
                        .build()) {
                    HttpPost postRequest = new HttpPost(
                            "http://localhost:7070/");
                    postRequest.setEntity(new StringEntity("11"));
                    HttpResponse response = httpclient.execute(postRequest);
                    assertEquals(200, response.getStatusLine().getStatusCode());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }


        ExecutorService service = Executors.newFixedThreadPool(10);

        for (int i = 0; i < 10; i++) {
            service.submit(new Requester());
        }

        Function[] dfgh = {x -> "h", y -> "j"};

        service.shutdown();
        service.awaitTermination(10, TimeUnit.SECONDS);
    }


}
