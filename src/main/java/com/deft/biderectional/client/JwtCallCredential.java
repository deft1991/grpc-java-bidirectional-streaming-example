package com.deft.biderectional.client;

import io.grpc.*;

import java.util.concurrent.Executor;

/**
 * @author Sergey Golitsyn
 * created on 13.11.2020
 */
public class JwtCallCredential extends CallCredentials {
    private final String jwt;

    public JwtCallCredential(String jwt) {
        this.jwt = jwt;
    }

    @Override public void applyRequestMetadata(RequestInfo requestInfo, Executor executor,
                                               MetadataApplier metadataApplier) {
        executor.execute(new Runnable() {
            @Override public void run() {
                try {
                    Metadata headers = new Metadata();
                    Metadata.Key<String> jwtKey = Metadata.Key.of("roomId", Metadata.ASCII_STRING_MARSHALLER);
                    headers.put(jwtKey, jwt);
                    metadataApplier.apply(headers);
                } catch (Throwable e) {
                    metadataApplier.fail(Status.UNAUTHENTICATED.withCause(e));
                }
            }
        });
    }

    @Override public void thisUsesUnstableApi() {
    }
}
