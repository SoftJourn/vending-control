package com.softjourn;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

@Slf4j
public class HttpVendRequestHandler implements HttpHandler, RequestsHolder {

    private BlockingDeque<HttpExchange> deque;

    private ConcurrentHashMap<HttpExchange, RequestProcessor.Status> inProcess;

    private int queueSizeLimit;

    public HttpVendRequestHandler(int queueSizeLimit) {
        this.queueSizeLimit = queueSizeLimit;
        deque = new LinkedBlockingDeque<>();
        inProcess = new ConcurrentHashMap<>();
    }

    @Override
    public synchronized void handle(HttpExchange httpExchange) throws IOException {
        synchronized (httpExchange) {
            log.info("Request received. IP " + httpExchange.getRemoteAddress());
            if (deque.size() >= queueSizeLimit ) { // allow only on
                httpExchange.sendResponseHeaders(509, 0);
                httpExchange.close();
            } else {
                deque.push(httpExchange);
                try {
                    httpExchange.wait();
                    RequestProcessor.Status status = inProcess.get(httpExchange);
                    httpExchange.sendResponseHeaders(status.code(), 0);
                    httpExchange.close();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public HttpExchange next() {
        try {
            return deque.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void putResult(HttpExchange exchange, RequestProcessor.Status status) {
        inProcess.put(exchange, status);
    }

}
