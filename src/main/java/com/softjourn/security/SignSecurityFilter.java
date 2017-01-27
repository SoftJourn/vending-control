package com.softjourn.security;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Optional.ofNullable;


@Slf4j
public class SignSecurityFilter extends Filter {

    private static final String AUTH_HEADER_NAME = "Authorization";

    private Signature signature;

    private AtomicInteger counter = new AtomicInteger(0);

    private Set<Instant> used = new HashSet<>();

    public SignSecurityFilter(byte[] publicKeyData) {
        try {
            X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(publicKeyData);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey pubKey = keyFactory.generatePublic(pubKeySpec);
            signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(pubKey);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            //should never happen
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Wrong key specified!");
        }
    }

    public SignSecurityFilter(Signature signature) {
        this.signature = signature;
    }

    public SignSecurityFilter(Set<Instant> used, Signature signature) {
        this.used = used;
        this.signature = signature;
    }

    @Override
    public void doFilter(HttpExchange httpExchange, Chain chain) throws IOException {
        try {
            processAuth(httpExchange);
            chain.doFilter(httpExchange);
        } catch (AccessControlException e) {
            httpExchange.sendResponseHeaders(401, 0);
            httpExchange.getResponseBody().write(e.getMessage().getBytes());
            httpExchange.close();
            log.warn("Authorization failure for IP " + httpExchange.getRemoteAddress() + ". " + e.getMessage());
        }
    }

    private void processAuth(HttpExchange httpExchange) {
        String authHeader = ofNullable(httpExchange.getRequestHeaders())
                .flatMap(h -> ofNullable(h.get(AUTH_HEADER_NAME)))
                .flatMap(h -> ofNullable(h.get(0)))
                .orElseThrow(() -> new AccessControlException("Auth header not provided."));

        String data = getData(authHeader);
        String signed = getSigned(authHeader);
        verifySignature(data, signed);
        verifyTime(data);
        verifyBody(data, httpExchange);
    }

    private void verifyBody(String data, HttpExchange httpExchange) {
        try {
            String body = IOUtils.toString(httpExchange.getRequestBody(), "utf8");
            if (!body.trim().isEmpty()) {
                verifyCell(data, body);
                httpExchange.setAttribute("Cell", body);
            }
        } catch (IOException e) {
            log.warn("Can't read request body", e);
        }
    }

    private void verifySignature(String data, String signed) {
        try {
            signature.update(data.getBytes());
            if (!signature.verify(new BigInteger(signed, 16).toByteArray())) {
                throw new AccessControlException("Error checking authorization.");
            }
        } catch (SignatureException e) {
            throw new AccessControlException("Error checking authorization.");
        }
    }

    boolean verifyTime(String data) {
        try {
            Long timestamp = Long.parseLong(data.substring(0, data.length() - 2));
            Instant time = Instant.ofEpochMilli(timestamp);
            Instant now = Instant.now();
            if (time.isAfter(now) || time.isBefore(now.minusSeconds(10)) || used.contains(time)) {
                throw new AccessControlException("Error checking authorization.");
            }
            used.add(time);
            cleanUsed();
            return true;
        } catch (NumberFormatException e) {
            throw new AccessControlException("Wrong auth header provided");
        }
    }

    boolean verifyCell(String data, String body) {
        String headerCell = data.substring(data.length() - 2);
        if (!headerCell.trim().equalsIgnoreCase(body.trim())) {
            throw new AccessControlException("Error checking authorization.");
        }
        return true;
    }

    private void cleanUsed() {
        if (counter.incrementAndGet() > 10) {
            used.removeIf(time -> time.isBefore(Instant.now().minusSeconds(10)));
        }
    }

    private String getSigned(String authHeader) {
        String[] headerArray = authHeader.split("\\.");
        if (headerArray.length != 2) throw new AccessControlException("Wrong auth header provided");
        return headerArray[1];
    }

    private String getData(String authHeader) {
        return authHeader.split("\\.")[0];
    }

    @Override
    public String description() {
        return "Filter to check signed response.";
    }

}
