package com.softjourn;

import com.sun.net.httpserver.HttpExchange;

public interface RequestsHolder {

    HttpExchange next();

    void putResult(HttpExchange exchange, RequestProcessor.Status status);
}
