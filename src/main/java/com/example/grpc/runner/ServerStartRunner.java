package com.example.grpc.runner;

import io.grpc.Server;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Profile("!test")
@Component
@RequiredArgsConstructor
public class ServerStartRunner implements ApplicationRunner, DisposableBean {

    private final Server grpcServer;

    // grpc는 별개의 netty서버를 구동시켜야 하므로 runner 사용
    @Override
    public void run(ApplicationArguments args) throws Exception {
        grpcServer.start();
        grpcServer.awaitTermination();
    }

    @Override
    public void destroy() throws Exception {
        if (!ObjectUtils.isEmpty(grpcServer)) {
            grpcServer.shutdown();
        }
    }
}