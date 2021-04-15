package com.example.grpc.service;

import com.dealicious.grpc.GrpcModel;
import com.dealicious.grpc.GrpcServiceGrpc;
import com.example.grpc.interceptor.GrpcInterceptor;
import com.google.common.util.concurrent.*;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.Executors;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest
public class GrpcTest {

    Logger log = LoggerFactory.getLogger(this.getClass());

    // 구글에서 제공되는 테스트용 api , 안타깝게도 아직 junit4밖에 지원하지 않는다.
    @Rule
    public GrpcCleanupRule grpcCleanUp = new GrpcCleanupRule();
    private final String serverName = InProcessServerBuilder.generateName();
    private final InProcessServerBuilder serverBuilder = InProcessServerBuilder.forName(serverName).directExecutor();
    private final InProcessChannelBuilder channelBuilder = InProcessChannelBuilder.forName(serverName).directExecutor();

    @Autowired
    GrpcService service;
    @Autowired
    GrpcInterceptor interceptor;

    // 구글에서는 하나의 서버에 하나의 채널 사용을 권고
    ManagedChannel channel;

    @Before
    public void before() throws IOException {
        grpcCleanUp.register(serverBuilder.addService(service).intercept(interceptor).build().start());
        channel = grpcCleanUp.register(channelBuilder.build());
    }

    /***
     * blockingStub : 1:1, 1:n 2가지의 통신형태를 지원
     */
    @Test
    public void blocking() {

        GrpcServiceGrpc.GrpcServiceBlockingStub blockingClient = GrpcServiceGrpc.newBlockingStub(channel);

        GrpcModel.Request request = GrpcModel.Request.newBuilder()
                //.setLongValue(1L)
                //.setStringValue("stringValue")
                .build();

        // 1:1
        GrpcModel.Response response = blockingClient.getOne(request);
        log.info(String.valueOf(response.getValueList()));

        // 1:n
        Iterator<GrpcModel.Response> itResponse = blockingClient.serverStream(request);
        itResponse.forEachRemaining(res -> {
            log.info(String.valueOf(res.getValueList()));
        });
    }

    /***
     * futureStub : 현재 1:1 방식만 지원, 이름그대로 future로 사용할때
     */
    @Test
    public void future() {

        GrpcServiceGrpc.GrpcServiceFutureStub futureClient = GrpcServiceGrpc.newFutureStub(channel);

        GrpcModel.Request request = GrpcModel.Request.newBuilder().setLongValue(1L).setStringValue("stringValue").build();

        ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1));
        ListenableFuture<GrpcModel.Response> future = futureClient.getOne(request);
        Futures.addCallback(future, new FutureCallback<GrpcModel.Response>() {
            @Override
            public void onSuccess(@NullableDecl GrpcModel.Response result) {
                log.info(String.valueOf(result.getValueList()));
            }

            @Override
            public void onFailure(Throwable t) {
            }
        }, executor);
    }


    /***
     * asyncStub : 1:1, n:1, 1:n, n:n 4가지 방식지원
     */
    @Test
    public void async() throws InterruptedException {

        GrpcServiceGrpc.GrpcServiceStub asyncClient = GrpcServiceGrpc.newStub(channel);

        GrpcModel.Request request = GrpcModel.Request.newBuilder().setLongValue(1L).setStringValue("stringValue").build();

        // stream 구독처리
        StreamObserver<GrpcModel.Response> responseObserver = new StreamObserver<GrpcModel.Response>() {
            @Override
            public void onNext(GrpcModel.Response res) {
                log.info(String.valueOf(res.getValueList()));
            }
            @Override
            public void onError(Throwable t) { }
            @Override
            public void onCompleted() { }
        };

        // 1:1
        asyncClient.getOne(request, responseObserver);

        // n:1
        StreamObserver<GrpcModel.Request> reqeustObserver = asyncClient.clientStream(responseObserver);
        reqeustObserver.onNext(request);
        reqeustObserver.onNext(request);
        reqeustObserver.onNext(request);
        reqeustObserver.onNext(request);
        reqeustObserver.onCompleted();

        // 1:n
        asyncClient.serverStream(request, responseObserver);

        // n:n
        reqeustObserver = asyncClient.biStream(responseObserver);
        reqeustObserver.onNext(request);
        reqeustObserver.onCompleted();

        // async 확인용
        Thread.sleep(1000);
    }

    @Test
    public void metaData() throws IOException {

        // http header 처럼 사용할수 있는 클래스
        Metadata metadata = new Metadata();

        // MetadataUtils.attachHeaders api를 사용하여 stub객체와 연결
        GrpcServiceGrpc.GrpcServiceBlockingStub client = MetadataUtils.attachHeaders(GrpcServiceGrpc.newBlockingStub(channel), metadata);
        metadata.put(Metadata.Key.of("app-transaction", Metadata.ASCII_STRING_MARSHALLER), "transactionValue");
    }
}