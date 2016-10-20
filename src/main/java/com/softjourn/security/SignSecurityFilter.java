package com.softjourn.security;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import static java.util.Optional.ofNullable;

public class SignSecurityFilter extends Filter {

    private static final String AUTH_HEADER_NAME = "Authorization";

    private Signature signature;

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
        verify(data, signed);

    }

    void verify(String data, String signed) {
        try {
            signature.update(data.getBytes());
            if (! signature.verify(signed.getBytes())) throw new AccessControlException("Error checking authorization.");
        } catch (SignatureException e) {
            throw new AccessControlException("Error checking authorization.");
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