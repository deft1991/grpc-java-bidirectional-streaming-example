package com.deft.biderectional.client;

import com.google.common.primitives.Ints;
import com.google.protobuf.ByteString;
import com.projectx.interop.BattleServiceGrpc;
import com.projectx.interop.BattleServiceOuterClass;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class GrpcBattleClientProjectX {

    //    public static final String HOST = "localhost";
    public static final String HOST = "projectx-dev-02.ext.i-free.ru";
    public static final int THREADS_COUNT = 2000;

    public static void main(String[] args) {


        ExecutorService executorService = Executors.newCachedThreadPool();
        int requestsCount = 10000;
        CountDownLatch finishedLatch = new CountDownLatch(requestsCount);
        ManagedChannel channel = ManagedChannelBuilder.forAddress(HOST, 6564).usePlaintext().build();

        List<StreamObserver<BattleServiceOuterClass.PlainData>> observers = new ArrayList<>();
        for (int j = 0; j < THREADS_COUNT; j++) {
            JwtCallCredential callCredential = new JwtCallCredential(String.valueOf((int) j / 2));
            executorService.execute(() -> {
                BattleServiceGrpc.BattleServiceStub service = BattleServiceGrpc.newStub(channel).withCallCredentials(callCredential);


                AtomicReference<StreamObserver<BattleServiceOuterClass.PlainData>> requestObserverRef = new AtomicReference<>();
                StreamObserver<BattleServiceOuterClass.PlainData> observer = getNewStreamObserver(service, requestObserverRef, finishedLatch);
                requestObserverRef.set(observer);
                BattleServiceOuterClass.PlainData requestCall = BattleServiceOuterClass.PlainData
                        .newBuilder()
                        .setData(ByteString.copyFrom(Ints.toByteArray(1))).build();
                observer.onNext(requestCall);
                try {
                    finishedLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                observer.onCompleted();
                observers.add(observer);
            });
        }
    }

    private static StreamObserver<BattleServiceOuterClass.PlainData> getNewStreamObserver(BattleServiceGrpc.BattleServiceStub service, AtomicReference<StreamObserver<BattleServiceOuterClass.PlainData>> requestObserverRef, CountDownLatch finishedLatch) {
        return service.echo(new StreamObserver<BattleServiceOuterClass.PlainData>() {


            @Override
            public void onNext(BattleServiceOuterClass.PlainData value) {
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                requestObserverRef.get().onNext(value);
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("on error");
                t.printStackTrace();
            }

            @Override
            public void onCompleted() {
                System.out.println("on completed");
                finishedLatch.countDown();
            }
        });
    }
}
