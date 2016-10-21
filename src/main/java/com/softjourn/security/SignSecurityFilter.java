package com.softjourn.security;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Optional.ofNullable;

public class SignSecurityFilter extends Filter {

    private static final String AUTH_HEADER_NAME = "Authorization";

    private Signature signature;

    private AtomicInteger counter = new AtomicInteger(0);

    private Set<Instant> used = new HashSet<>();

    public SignSecurityFilter(byte[] publicKeyData) {
        try {
            X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(publicKeyData);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA", "BC");
            PublicKey pubKey = keyFactory.generatePublic(pubKeySpec);
            signature = Signature.getInstance("SHA1withRSA", "BC");
            signature.initVerify(pubKey);
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException e) {
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
        String authHeader = ofNullable(httpExchange.getRequestHeaders())
                .map(h -> h.get(AUTH_HEADER_NAME))
                .map(h -> h.get(0))
                .orElseThrow(() -> new AccessControlException("Auth header not provided."));
    }

    void processAuth(HttpExchange httpExchange) {
        String authHeader = ofNullable(httpExchange.getRequestHeaders())
                .flatMap(h -> ofNullable(h.get(AUTH_HEADER_NAME)))
                .flatMap(h -> ofNullable(h.get(0)))
                .orElseThrow(() -> new AccessControlException("Auth header not provided."));

        String data = getData(authHeader);
        String signed = getSigned(authHeader);
        verifySignature(data, signed);

    }

    void verifySignature(String data, String signed) {
        try {
            signature.update(data.getBytes());
            if (! signature.verify(signed.getBytes())) throw new AccessControlException("Error checking authorization.");
        } catch (SignatureException e) {
            throw new AccessControlException("Error checking authorization.");
        }
    }

    boolean verifyTime(String data) {
        try {
            Long timestamp = Long.parseLong(data);
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

    private void cleanUsed() {
        if (counter.incrementAndGet() > 10) {
            Iterator<Instant> usedIterator = used.iterator();
            while (usedIterator.hasNext()) {
                Instant time = usedIterator.next();
                if (time.isBefore(Instant.now().minusSeconds(10))) {
                    usedIterator.remove();
                }
            }
        }
    }

    private String getSigned(String authHeader) {
        String[] headerArray =  authHeader.split("\\.");
        if(headerArray.length != 2) throw new AccessControlException("Wrong auth header provided");
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
